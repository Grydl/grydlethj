package grydl.service;

import org.ethereum.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

/**
 * Created by azanlekor on 26.09.16.
 */
@Service
public class WalletService implements IWalletService{

    @Override
    public ECKey createElectronicKey(){
        return new ECKey();
    }

    @Override
    public String createPrivateKEy(ECKey key){
        byte[] priv = key.getPrivKeyBytes();
        return Hex.toHexString(priv);
    }

    @Override
    public String createPublicKey(ECKey key){
        byte[] addr = key.getAddress();
        return Hex.toHexString(addr);
    }

}
