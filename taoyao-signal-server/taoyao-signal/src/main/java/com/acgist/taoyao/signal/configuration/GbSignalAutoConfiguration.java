package com.acgist.taoyao.signal.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.acgist.taoyao.boot.config.GbProperties;
import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.runner.OrderedCommandLineRunner;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.gb.GbServer;
import com.acgist.taoyao.signal.client.gb.IGbServer;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "taoyao.gb", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GbSignalAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IGbServer gbServer(
        GbProperties       gbProperties,
        ClientManager      clientManager,
        ProtocolManager    protocolManager,
        SecurityProperties securityProperties
    ) {
        log.info("注册GB信令");
        return new GbServer(gbProperties, clientManager, protocolManager, securityProperties);
    }

    @Bean
    @ConditionalOnBean(IGbServer.class)
    public CommandLineRunner gbServerCommandLineRunner(IGbServer gbServer) {
        return new OrderedCommandLineRunner() {
            @Override
            public void run(String ... args) throws Exception {
                gbServer.init();
                gbServer.registerServer();
            }
        };
    }

}
