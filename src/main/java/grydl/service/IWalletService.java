package grydl.service;

import org.ethereum.crypto.ECKey;

/**
 * Created by azanlekor on 26.09.16.
 */
public interface IWalletService {

    ECKey createElectronicKey();

    String createPrivateKEy(ECKey key);

    String createPublicKey(ECKey key);
}
