package grydl.ethereum;

import grydl.domain.EthTransaction;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.facade.Repository;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.rlpx.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Future;

/**
 * Ethereum Bean Class
 * The Class for Ethereum node and element
 */
public class EthereumBean implements IEthereum {

    //Logger to log information
    private Logger logger = LoggerFactory.getLogger(EthereumBean.class);

    //Ethereum Node classe
    protected Ethereum ethereum;

    //Confifiguration of Ethereum Node
    @Autowired
    protected SystemProperties config;

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

    private volatile long txCount;
    private volatile long gasSpent;

    public void start() {

        logger.info("####### Initialisation Ethereum Bean ... ");
        //Ethreum Class creation
        this.ethereum = EthereumFactory.createEthereum();

        //Creation of Ethreum listener to handdle event on Ethereum Node
        EthereumListener ethereumListener = new EthereumListener(ethereum);

        ethereumListener.setNodesDiscovered(this.nodesDiscovered);
        ethereumListener.setSynced(this.synced);
        this.ethereum.addListener(ethereumListener);
    }


    public void initialisation(){
        try {
            logger.debug("Ethereum Work Thread started....");

            if (config.peerDiscovery()) {
                waitForDiscovery();
            } else {
                logger.info("Peer discovery disabled. We should actively connect to another peers or wait for incoming connections");
            }

            waitForAvailablePeers();

            waitForSyncPeers();

            waitForFirstBlock();

            //waitForSync();

            onSyncDone();

        } catch (Exception e) {
            logger.error("Error occurred : ", e);
        }
    }


    /**
     * This method all to get the best block
     * @return
     */
    @Override
    public String getBestBlock(){
        return " Genesis "+config.genesisInfo()+"NetworkID "+config.networkId()+"  " + ethereum.getBlockchain().getBestBlock().getNumber();
    }

    @Override
    public Future<Transaction> sendTransactionAsync(String privatekey, String receiver, Double amount) throws InterruptedException {
        return null;
    }

    @Override
    public void sendTransaction(String privatekey, String receiver, Double amount) {

    }

    @Override
    public Double getBalance(String receiver) {
        return null;
    }

    @Override
    public List<EthTransaction> getListTransactionState() {
        return null;
    }


    public String getBalance(){
        byte[] cow = Hex.decode("5db10750e8caff27f906b41c71b3471057dd2004");

        // Get snapshot some time ago - 10% blocks ago
        long bestNumber = ethereum.getBlockchain().getBestBlock().getNumber();
        long oldNumber = (long) (bestNumber * 0.9);

        Block oldBlock = ethereum.getBlockchain().getBlockByNumber(oldNumber);

        Repository repository = ethereum.getRepository();
        Repository snapshot = ethereum.getSnapshotTo(oldBlock.getStateRoot());

        BigInteger nonce_ = snapshot.getNonce(cow);
        BigInteger nonce = repository.getNonce(cow);

        System.err.println(" ##########" + ethereum.getBlockchain().getBestBlock().getNumber() + " [cd2a3d9] => snapshot_nonce:" +  nonce_ + " latest_nonce:" + nonce);

        return nonce.toString();

    }

    /**
     * Is called when the whole blockchain sync is complete
     */
    public void onSyncDone() throws Exception {
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



}
