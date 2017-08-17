package grydl.ethereum;

import grydl.domain.EthTransaction;
import org.ethereum.core.Block;
import org.ethereum.core.PendingState;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.Channel;
import org.ethereum.util.BIUtil;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class EthereumListener extends EthereumListenerAdapter {

    Logger logger = LoggerFactory.getLogger(EthereumListener.class);

    Ethereum ethereum;

    private boolean syncDone = false;

    private volatile long txCount;

    private volatile long gasSpent;

    protected List<Node> nodesDiscovered = new Vector<>();

    private EthereumNode ethereumNode;


    protected Map<Node, StatusMessage> ethNodes = new Hashtable<>();

    protected List<Node> syncPeers = new Vector<>();

    boolean synced = false;
    boolean syncComplete = false;

    protected Block bestBlock = null;


    public EthereumListener(Ethereum ethereum) {
        this.ethereum = ethereum;
    }

    @Override
    public void onBlock(Block block, List<TransactionReceipt> receipts) {

        bestBlock = block;
        txCount += receipts.size();
        for (TransactionReceipt receipt : receipts) {
            gasSpent += ByteUtil.byteArrayToLong(receipt.getGasUsed());
        }
        if (syncComplete) {
            logger.info("New block: " + block.getShortDescr());
        }

        logger.debug("Do something on block: " + block.getNumber());
        if (syncDone)
            calcNetHashRate(block);
    }

    @Override
    public void onPendingTransactionsReceived(List<Transaction> transactions) {
        for (Transaction tx : transactions) {
            //ethereum.onPendingTransactionReceived(tx);
            logger.info(" ############## ON PENDING TRANSACTION "+tx.toString());
        }
    }


    @Override
    public void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block) {

        if(this.ethereumNode.getTxClose().containsKey(Hex.toHexString(txReceipt.getTransaction().getHash()))
                || this.ethereumNode.getTxWaiters().containsKey(Hex.toHexString(txReceipt.getTransaction().getHash()))){
            EthTransaction ethTransaction = new EthTransaction();
            ethTransaction.setTransactionHash(Hex.toHexString(txReceipt.getTransaction().getHash()));
            ethTransaction.setTransaction(txReceipt.getTransaction().toString());
            ethTransaction.setPendingTransactionState(state);
            ethTransaction.setTxError(txReceipt.getError());
            this.ethereumNode.getTxWaiters().put(ethTransaction.getTransactionHash(),ethTransaction);
            this.ethereumNode.getTxClose().remove(ethTransaction.getTransactionHash());
            logger.debug("############ STATE "+state.toString());
            logger.debug("############  TX RECEIPT  "+txReceipt.toString());
            logger.debug("############  TX RECEIPT  ERROR "+txReceipt.getError());
        }

    }



    /**
     *  Mark the fact that you are touching
     *  the head of the chain
     */
    @Override
    public void onSyncDone() {
        logger.info(" ** SYNC COMPLETE DONE ** ");
        syncDone = true;
    }


    @Override
    public void onNodeDiscovered(Node node) {
        if (nodesDiscovered.size() < 1000) {
            nodesDiscovered.add(node);
        }
    }

    @Override
    public void onEthStatusUpdated(Channel channel, StatusMessage statusMessage) {
        ethNodes.put(channel.getNode(), statusMessage);
    }

    @Override
    public void onPeerAddedToSyncPool(Channel peer) {
        syncPeers.add(peer.getNode());
    }

    /**
     * Just small method to estimate total power off all miners on the net
     * @param block
     */
    private void calcNetHashRate(Block block){

        if ( block.getNumber() > 1000){

            long avgTime = 1;
            long cumTimeDiff = 0;
            Block currBlock = block;
            for (int i=0; i < 1000; ++i){

                Block parent = ethereum.getBlockchain().getBlockByHash(currBlock.getParentHash());
                long diff = currBlock.getTimestamp() - parent.getTimestamp();
                cumTimeDiff += Math.abs(diff);
                currBlock = parent;
            }

            avgTime = cumTimeDiff / 1000;

            BigInteger netHashRate = block.getDifficultyBI().divide(BIUtil.toBI(avgTime));
            double hashRate = netHashRate.divide(new BigInteger("1000000000")).doubleValue();

            System.out.println("Net hash rate: " + hashRate + " GH/s");
        }

    }

    public List<Node> getNodesDiscovered() {
        return nodesDiscovered;
    }

    public void setNodesDiscovered(List<Node> nodesDiscovered) {
        this.nodesDiscovered = nodesDiscovered;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public boolean isSyncComplete() {
        return syncComplete;
    }

    public void setSyncComplete(boolean syncComplete) {
        this.syncComplete = syncComplete;
    }

    public EthereumNode getEthereumNode() {
        return ethereumNode;
    }

    public void setEthereumNode(EthereumNode ethereumNode) {
        this.ethereumNode = ethereumNode;
    }
}
