package com.acgist.taoyao.signal.configuration;

import java.util.Base64;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.acgist.taoyao.boot.config.SocketProperties;
import com.acgist.taoyao.boot.runner.OrderedCommandLineRunner;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.socket.SocketSignal;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令自动配置
 * 
 * @author acgist
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "taoyao.socket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SocketSignalAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SocketSignal socketSignal(
        ClientManager         clientManager,
        ProtocolManager       protocolManager,
        SocketProperties      socketProperties,
        PlatformErrorProtocol platformErrorProtocol
    ) {
        this.buildSecret(socketProperties);
        return new SocketSignal(clientManager, protocolManager, socketProperties, platformErrorProtocol);
    }
    
    @Bean
    @ConditionalOnBean(SocketSignal.class)
    public CommandLineRunner socketSignalCommandLineRunner(SocketSignal socketSignal) {
        return new OrderedCommandLineRunner() {
            @Override
            public void run(String ... args) throws Exception {
                socketSignal.init();
            }
        };
    }

    /**
     * @param socketProperties 加密配置
     */
    private void buildSecret(SocketProperties socketProperties) {
        log.info("Socket信令加密策略：{}", socketProperties.getEncrypt());
        if(socketProperties.getEncrypt() == null) {
            log.info("Socket信令加密策略为空使用明文");
            return;
        }
        if(StringUtils.isNotEmpty(socketProperties.getEncryptSecret())) {
            log.info("Socket信令加密密码（固定）：{}", socketProperties.getEncryptSecret());
            return;
        }
        final byte[] bytes = switch (socketProperties.getEncrypt()) {
        case AES -> new byte[16];
        case DES -> new byte[8];
        default  -> null;
        };
        if(bytes == null) {
            final Random random = new Random();
            random.nextBytes(bytes);            socketProperties.setEncryptSecret(Base64.getMimeEncoder().encodeToString(bytes));
            log.info("Socket信令加密密码（随机）：{}", socketProperties.getEncryptSecret());
        } else {
            log.warn("Socket信令加密密码算法不支持的算法：{}", socketProperties.getEncrypt());
        }
    }
    
}
