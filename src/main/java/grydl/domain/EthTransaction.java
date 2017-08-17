package grydl.domain;

import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.listener.EthereumListener;

/**
 * Created by azanlekor on 23.10.16.
 */
public class EthTransaction {

    String transactionHash;

    String transaction;

    EthereumListener.PendingTransactionState pendingTransactionState  = EthereumListener.PendingTransactionState.NEW_PENDING;

    String state = pendingTransactionState.name();

    String txError;

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public EthereumListener.PendingTransactionState getPendingTransactionState() {
        return pendingTransactionState;
    }

    public void setPendingTransactionState(EthereumListener.PendingTransactionState pendingTransactionState) {
        this.pendingTransactionState = pendingTransactionState;
    }

    public String getState() {
        return pendingTransactionState.name();
    }

    public String getTxError() {
        return txError;
    }

    public void setTxError(String txError) {
        this.txError = txError;
    }
}
