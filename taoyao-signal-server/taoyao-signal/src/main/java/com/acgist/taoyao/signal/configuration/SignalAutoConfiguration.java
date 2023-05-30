package com.acgist.taoyao.signal.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.acgist.taoyao.boot.runner.OrderedCommandLineRunner;
import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * 信令自动配置
 * 
 * @author acgist
 */
@AutoConfiguration
@RequiredArgsConstructor
public class SignalAutoConfiguration {

    private final ApplicationContext applicationContext;
    
    @PostConstruct
    public void init() {
        EventPublisher.setApplicationContext(this.applicationContext);
    }
    
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
