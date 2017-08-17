package grydl.ethereum;

import grydl.constant.EthereumConst;
import grydl.domain.EthTransaction;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.PendingState;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.mine.MinerListener;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.samples.SendTransaction;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Ethereum Bean Class
 * The Class for Ethereum node and element
 */
public class EthereumNode implements MinerListener,IEthereum {

    //Logger to log information
    private Logger logger = LoggerFactory.getLogger(EthereumNode.class);

    //Ethereum Node classe
    protected Ethereum ethereum;

    //Configuration of Ethereum Node
    @Autowired
    protected SystemProperties config;

    //@Autowired
    PendingState pendingState;

    public Ethereum getEthereum() {
        return ethereum;
    }

    public void setEthereum(Ethereum ethereum) {
        this.ethereum = ethereum;
    }

    public SystemProperties getConfig() {
        return config;
    }

    public void setConfig(SystemProperties config) {
        this.config = config;
    }

    protected boolean synced = false;

    protected boolean syncComplete = false;

    protected Block bestBlock = null;

    protected List<Node> nodesDiscovered = new Vector<>();

    protected List<Node> syncPeers = new Vector<>();

    protected Map<Node, StatusMessage> ethNodes = new Hashtable<>();


    // remember here what transactions have been sent
    // removing transaction from here on block arrival
    private Map<ByteArrayWrapper, Transaction> pendingTxs = Collections.synchronizedMap(
            new HashMap<ByteArrayWrapper, Transaction>());

    //Awaiting transaction
    private Map<String, EthTransaction> txWaiters =
            Collections.synchronizedMap(new HashMap<String, EthTransaction>());

    //Transaction include or aborted
    private Map<String, EthTransaction> txClose =
            Collections.synchronizedMap(new HashMap<String, EthTransaction>());

    private volatile long txCount;
    private volatile long gasSpent;

    /**
     * This method start Ethereum Node
     */
    public void start() {

        logger.info("############## ETHEREUM NODE INITIALIZATION ..... ");
        //Ethereum creation
        this.ethereum = EthereumFactory.createEthereum();

        //Creation of Ethreum listener to handdle event on Ethereum Node
        EthereumListener ethereumListener = new EthereumListener(ethereum);

        ethereumListener.setNodesDiscovered(this.nodesDiscovered);
        ethereumListener.setSynced(this.synced);
        ethereumListener.setEthereumNode(this);
        this.ethereum.addListener(ethereumListener);

    }


    /**
     * This method try to discover Ethereum peer and make the synchonization
     */
    public void synchronization(){
        try {
            logger.debug("Ethereum Synchonization Node Start ....");

            if (config.peerDiscovery()) {
                waitForDiscovery();
            } else {
                logger.info("Peer discovery disabled. We should actively connect to another peers or wait for incoming connections");
            }

            waitForAvailablePeers();

            waitForSyncPeers();

            waitForFirstBlock();

            waitForSync();

            onSyncDone();

        } catch (Exception e) {
            logger.error("Error occurred : ", e);
        }
    }


    /**
     * This method get the best block
     * @return
     */
    public String getBestBlock(){
        return " Genesis "+config.genesisInfo()+"  " + ethereum.getBlockchain().getBestBlock().getNumber();
    }


    /**
     * Is called when the whole blockchain sync is complete
     */
    public void onSyncDone() throws Exception {
        logger.info("Synchonization complete ...... ");
        logger.debug("Monitoring new blocks in real-time...");

    }



    /**
     * Waits until any new nodes are discovered by the UDP discovery protocol
     */
    protected void waitForDiscovery() throws Exception {
        logger.info("Waiting for nodes discovery...");

        int bootNodes = config.peerDiscoveryIPList().size() + 1; // +1: home node
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 300 : 5000);

            if (nodesDiscovered.size() > bootNodes) {
                logger.info("[v] Discovery works, new nodes started being discovered.");
                return;
            }

            if (cnt >= 30) logger.warn("Discovery keeps silence. Waiting more...");
            if (cnt > 50) {
                logger.error("Looks like discovery failed, no nodes were found.\n" +
                        "Please check your Firewall/NAT UDP protocol settings.\n" +
                        "Your IP interface was detected as " + config.bindIp() + ", please check " +
                        "if this interface is correct, otherwise set it manually via 'peer.discovery.bind.ip' option.");
                throw new RuntimeException("Discovery failed.");
            }
            cnt++;
        }
    }


    /**
     * When live nodes found SyncManager should select from them the most
     * suitable and add them as peers for syncing the blocks
     */
    protected void waitForSyncPeers() throws Exception {
        logger.info("Searching for peers to sync with...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 1000 : 5000);

            if (syncPeers.size() > 0) {
                logger.debug("[v] At least one sync peer found.");
                return;
            }

            if (cnt >= 30) logger.info("No sync peers found so far. Keep searching...");
            if (cnt > 60) {
                logger.debug("No sync peers found. Logs need to be investigated.");
//                throw new RuntimeException("Sync peers failed.");
            }
            cnt++;
        }
    }

    /**
     * Waits until blocks import started
     */
    protected void waitForFirstBlock() throws Exception {
        Block currentBest = ethereum.getBlockchain().getBestBlock();
        logger.info("Current BEST block: " + currentBest.getShortDescr());
        logger.info("Waiting for blocks start importing (may take a while)...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 300 ? 1000 : 60000);

            if (bestBlock != null && bestBlock.getNumber() > currentBest.getNumber()) {
                logger.debug("[v] Blocks import started.");
                return;
            }

            if (cnt >= 300) logger.info("Still no blocks. Be patient...");
            if (cnt > 330) {
                logger.error("No blocks imported during a long period. Must be a problem, logs need to be investigated.");
//                throw new RuntimeException("Block import failed.");
            }
            cnt++;
        }
    }

    /**
     * Waits until the whole blockchain sync is complete
     */
    private void waitForSync() throws Exception {
        logger.info("Waiting for the whole blockchain sync (will take up to several hours for the whole chain)...");
        while(true) {
            Thread.sleep(10000);

            if (synced) {
                logger.info("[v] Sync complete! The best block: " + bestBlock.getShortDescr());
                syncComplete = true;
                return;
            }

            logger.info("Blockchain sync in progress. Last imported block: " + bestBlock.getShortDescr() +
                    " (Total: txs: " + txCount + ", gas: " + (gasSpent / 1000) + "k)");
            txCount = 0;
            gasSpent = 0;
        }
    }

    /**
     * Discovering nodes is only the first step. No we need to find among discovered nodes
     * those ones which are live, accepting inbound connections, and has compatible subprotocol versions
     */
    protected void waitForAvailablePeers() throws Exception {
        logger.info("Waiting for available Eth capable nodes...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 1000 : 5000);

            if (ethNodes.size() > 0) {
                logger.debug("[v] Available Eth nodes found.");
                return;
            }

            if (cnt >= 30) logger.debug("No Eth nodes found so far. Keep searching...");
            if (cnt > 60) {
                logger.error("No eth capable nodes found. Logs need to be investigated.");
//                throw new RuntimeException("Eth nodes failed.");
            }
            cnt++;
        }
    }

    /**
     * This method help to send transaction to the Ethereum blockchain
     * @param privatekey
     * @param receiver
     * @param amount
     */
    @Override
    public Future<Transaction> sendTransactionAsync(String privatekey, String receiver, Double amount) throws InterruptedException {
        logger.info("Start generating transactions...");
        ECKey senderKey = ECKey.fromPrivate(Hex.decode(privatekey));
        byte[] senderAddr = senderKey.getAddress();
        byte[] receiverAddr = Hex.decode(receiver);

        Double amountDec = amount.doubleValue()*1000d;


        BigInteger etherToSend = BigInteger.valueOf(amountDec.longValue());
        // Weis in 1 ether

        BigInteger weiToSend = EthereumConst.WEIINETHER.multiply(etherToSend).divide(BigInteger.valueOf(1000));//this converts the whole number from "etherToSend" into wei

        BigInteger nonce = ethereum.getRepository().getNonce(senderAddr);

        Transaction tx = new Transaction(
                ByteUtil.bigIntegerToBytes(nonce),
                ByteUtil.longToBytesNoLeadZeroes(ethereum.getGasPrice()),
                ByteUtil.longToBytesNoLeadZeroes(200000),
                receiverAddr,
                ByteUtil.bigIntegerToBytes(weiToSend),
                new byte[0]);
        tx.sign(senderKey);
        logger.info("<=== Sending transaction: " + tx);

        this.getTxClose().put(Hex.toHexString(tx.getHash()),new EthTransaction());

        return ethereum.submitTransaction(tx);
    }

    public void sendTransaction(String privatekey, String receiver, Double amount){
        try {
            this.sendTransactionAsync(privatekey, receiver, amount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * This method help to get the balance
     * @param receiver
     * @return
     */
    public Double getBalance(String receiver){
        byte[] receiverAddr = Hex.decode(receiver);
        BigInteger balance = ethereum.getRepository().getBalance(receiverAddr);
        BigInteger balanceDec = balance.multiply(BigInteger.valueOf(1000)).divide(EthereumConst.WEIINETHER);
        return Double.valueOf(Optional.of(balanceDec.doubleValue()).orElse(0d)/1000d);
    }

    @Override
    public List<EthTransaction> getListTransactionState(){
        Map<String,EthTransaction> mapTransaction = getTxWaiters();
        Map<String,EthTransaction> txWaitersTemp = Collections.synchronizedMap(new HashMap<String, EthTransaction>());

        List<EthTransaction> listEthTransaction = new ArrayList<>();
        for(String t : mapTransaction.keySet() ){
            EthTransaction ethTransaction = mapTransaction.get(t);
            listEthTransaction.add(ethTransaction);
            if (ethTransaction.getPendingTransactionState().isPending()) txWaitersTemp.put(t,ethTransaction);
        }

        txWaiters.clear();
        setTxWaiters(txWaitersTemp);
        return  listEthTransaction;
    }


    @Override
    public void miningStarted() {
        logger.info("Miner started");
    }

    @Override
    public void miningStopped() {
        logger.info("Miner stopped");
    }

    @Override
    public void blockMiningStarted(Block block) {
        logger.info("Start mining block: " + block.getShortDescr());
    }

    @Override
    public void blockMined(Block block) {
        logger.info("Block mined! : \n" + block);
    }

    @Override
    public void blockMiningCanceled(Block block) {
        logger.info("Cancel mining block: " + block.getShortDescr());
    }


    public Map<String, EthTransaction> getTxClose() {
        return txClose;
    }

    public void setTxClose(Map<String, EthTransaction> txClose) {
        this.txClose = txClose;
    }

    public Map<String, EthTransaction> getTxWaiters() {
        return txWaiters;
    }

    public void setTxWaiters(Map<String, EthTransaction> txWaiters) {
        this.txWaiters = txWaiters;
    }
}
