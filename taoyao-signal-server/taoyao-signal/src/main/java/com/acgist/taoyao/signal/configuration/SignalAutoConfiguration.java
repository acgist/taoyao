package com.acgist.taoyao.signal.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.boot.runner.OrderedCommandLineRunner;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

/**
 * 信令自动配置
 * 
 * @author acgist
 */
@Configuration
public class SignalAutoConfiguration {

    @Bean
    @Autowired
    public CommandLineRunner signalCommandLineRunner(ProtocolManager protocolManager) {
        return new OrderedCommandLineRunner() {
            @Override
            public void run(String ... args) throws Exception {
                protocolManager.init();
            }
        };
    }
    
}
