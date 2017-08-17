package grydl.ethereum;

import grydl.domain.EthTransaction;
import org.ethereum.core.Transaction;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by azanlekor on 26.09.16.
 */
public interface IEthereum {

    void start();

    String getBestBlock();

    Future<Transaction> sendTransactionAsync(String privatekey, String receiver, Double amount) throws InterruptedException;

    void sendTransaction(String privatekey, String receiver, Double amount);

    Double getBalance(String receiver);

    List<EthTransaction> getListTransactionState();
}
