package grydl;

import grydl.blockchain.GrydlBlockchain;
import grydl.ethereum.EthereumBean;
import grydl.ethereum.EthereumMiner;
import grydl.ethereum.EthereumNode;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.PendingState;
import org.ethereum.core.PendingStateImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.concurrent.Executors;

@Configuration
public class Config {

    @Bean
    SystemProperties systemProperties(){
        return SystemProperties.getDefault();
    }

    //@Bean
    EthereumBean ethereumBean(){
        EthereumBean ethereumBean = new EthereumBean();
       // ethereumBean.setConfig(systemProperties());
        Executors.newSingleThreadExecutor().submit(ethereumBean::start);
        return ethereumBean;
    }

    @Bean
    EthereumNode ethereumNode(){
        EthereumNode ethereumNode = new EthereumNode();
        return ethereumNode;
    }

    @Bean
    GrydlBlockchain ethereumMiner(){
        EthereumMiner ethereumMiner = new EthereumMiner();
        ethereumMiner.setEthereumNode(ethereumNode());
        Executors.newSingleThreadExecutor().submit(ethereumMiner::start);
        return ethereumMiner;
    }


}
