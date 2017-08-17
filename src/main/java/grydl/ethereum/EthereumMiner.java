package grydl.ethereum;

import grydl.domain.EthTransaction;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.mine.Ethash;
import org.ethereum.mine.MinerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import grydl.blockchain.GrydlBlockchain;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Ethereum Bean Class
 * The Class for Ethereum node and element
 */
public class EthereumMiner implements MinerListener,GrydlBlockchain {

    //Logger to log information
    private Logger logger = LoggerFactory.getLogger(EthereumMiner.class);

    @Autowired
    private EthereumNode ethereumNode;

    public EthereumNode getEthereumNode() {
        return ethereumNode;
    }

    public void setEthereumNode(EthereumNode ethereumNode) {
        this.ethereumNode = ethereumNode;
    }

    @Override
    public void miningStarted() {
        ethereumNode.miningStarted();
    }

    @Override
    public void miningStopped() {
        ethereumNode.miningStopped();
    }

    @Override
    public void blockMiningStarted(Block block) {
        ethereumNode.blockMiningStarted(block);

    }

    @Override
    public void blockMined(Block block) {
        ethereumNode.blockMined(block);
    }

    @Override
    public void blockMiningCanceled(Block block) {
        ethereumNode.blockMiningCanceled(block);
    }

    @Override
    public void start() {
        ethereumNode.start();

        logger.info("###################################################################");
        if (ethereumNode.getConfig().isMineFullDataset()) {
            logger.info("Generating Full Dataset (may take up to 10 min if not cached)...");
            Ethash ethash = Ethash.getForBlock(ethereumNode.getConfig(),ethereumNode.getEthereum().getBlockchain().getBestBlock().getNumber());
            ethash.getFullDataset();
            logger.info("Full dataset generated (loaded).");
        }
        ethereumNode.getEthereum().getBlockMiner().addListener(this);
        ethereumNode.getEthereum().getBlockMiner().startMining();
    }

    @Override
    public String getBestBlock() {
        return ethereumNode.getBestBlock();
    }

    @Override
    public void sendTransaction(String privatekey, String receiver, Double amount) {
        ethereumNode.sendTransaction(privatekey,receiver,amount);
    }

    @Override
    public Future<Transaction> sendTransactionAsync(String privatekey, String receiver, Double amount) throws InterruptedException {
        return ethereumNode.sendTransactionAsync(privatekey, receiver, amount);
    }

    @Override
    public Double getBalance(String receiver) {
        return ethereumNode.getBalance(receiver);
    }

    @Override
    public List<EthTransaction> getListTransaction(){
        return  ethereumNode.getListTransactionState();
    }


}
