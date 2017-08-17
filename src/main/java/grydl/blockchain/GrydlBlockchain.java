package grydl.blockchain;

import grydl.domain.EthTransaction;
import org.ethereum.core.Transaction;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by azanlekor on 27.09.16.
 */
public interface GrydlBlockchain {

    void start();

    String getBestBlock();

    void sendTransaction(String privatekey, String receiver, Double amount);

    Future<Transaction> sendTransactionAsync(String privatekey, String receiver, Double amount) throws InterruptedException;

    Double getBalance(String receiver);

    List<EthTransaction> getListTransaction();
}
