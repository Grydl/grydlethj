package grydl.rest;


import grydl.domain.EthTransaction;
import grydl.domain.Ethaccount;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.listener.EthereumListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import grydl.blockchain.GrydlBlockchain;
import grydl.service.WalletService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class GethjController {

    //Logger to log information
    private Logger logger = LoggerFactory.getLogger(GethjController.class);

    @Autowired
    GrydlBlockchain grydlBlockchain;

    @Autowired
    WalletService walletService;

    /**
     * This method return the Besto block of Ethereum Blockchain
     * @return
     * @throws IOException
     */
    // curl -w "\n" -X GET http://localhost:8080/bestBlock
    @RequestMapping(value = "/bestBlock", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBestBlock() throws IOException {
        return Optional.ofNullable(grydlBlockchain.getBestBlock())
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    /**
     * This method send transaction to the blockchain
     * @param ethaccount
     * @return
     * @throws Exception
     */
    //curl -w "\n" -X GET http://localhost:8080/sendTransaction
    @RequestMapping(value = "/sendEthaccount", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<EthTransaction> sendEthaccount(@RequestBody Ethaccount ethaccount) throws Exception {
        logger.debug("REST request to send transaction : {}", ethaccount);
        Future<Transaction> transactionFuture = grydlBlockchain.sendTransactionAsync(ethaccount.getPrivateKey(),ethaccount.getPublicKey(),ethaccount.getAmount());
        Transaction transaction = transactionFuture.get();
        EthTransaction ethTransaction = new EthTransaction();
        ethTransaction.setTransactionHash(Hex.toHexString(transaction.getHash()));
        ethTransaction.setPendingTransactionState(EthereumListener.PendingTransactionState.NEW_PENDING);
        ethTransaction.setTransaction(transaction.toString());


        return Optional.ofNullable(ethTransaction)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * This method generate public and private key
     * @return
     * @throws Exception
     */
    //curl -w "\n" -X GET http://localhost:8080/generateKey"
    @RequestMapping(value = "/generateKey", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Ethaccount> generateKeys() throws Exception {
        ECKey ecKey = walletService.createElectronicKey();
        String privateKEy = walletService.createPrivateKEy(ecKey);
        String publicKey = walletService.createPublicKey(ecKey);
        Ethaccount ethaccount = new Ethaccount();
        ethaccount.setPrivateKey(privateKEy);
        ethaccount.setPublicKey(publicKey);
        ethaccount.setBalance(0d);
        return Optional.ofNullable(ethaccount)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    //curl -w "\n" -X GET http://localhost:8080/getBalance/5db10750e8caff27f906b41c71b3471057dd2004
    //curl -w "\n" -X GET http://localhost:8080/getBalance/913ef78a556576145593b04a40c972f3fcde4ef7
    @RequestMapping(value = "/getBalance/{address}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBalance(@PathVariable String address) throws Exception {
        return Optional.ofNullable(grydlBlockchain.getBalance(address).toString())
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    //curl -w "\n" -X GET http://localhost:8080/getStateTransaction
    @RequestMapping(value = "/getStateTransaction", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EthTransaction>> getStateTransaction() throws Exception {
        return Optional.ofNullable(grydlBlockchain.getListTransaction())
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }




}
