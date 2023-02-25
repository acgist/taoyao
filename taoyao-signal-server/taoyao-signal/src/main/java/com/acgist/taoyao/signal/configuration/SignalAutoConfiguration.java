package com.acgist.taoyao.signal.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.acgist.taoyao.boot.runner.OrderedCommandLineRunner;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

/**
 * 信令自动配置
 * 
 * @author acgist
 */
@AutoConfiguration
public class SignalAutoConfiguration {

    @Bean
    public CommandLineRunner signalCommandLineRunner(ProtocolManager protocolManager) {
        return new OrderedCommandLineRunner() {
            @Override
            public void run(String ... args) throws Exception {
                protocolManager.init();
            }
        };
    }
    
}
