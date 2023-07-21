package com.acgist.taoyao.signal.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.acgist.taoyao.boot.config.ScriptProperties;
import com.acgist.taoyao.signal.protocol.platform.PlatformRebootProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformScriptProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformShutdownProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemRebootProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemShutdownProtocol;

/**
 * 脚本自动配置
 * 
 * @author acgist
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScriptAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SystemRebootProtocol systemRebootProtocol(ScriptProperties scriptProperties) {
        return new SystemRebootProtocol(scriptProperties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SystemShutdownProtocol systemShutdownProtocol(ScriptProperties scriptProperties) {
        return new SystemShutdownProtocol(scriptProperties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PlatformRebootProtocol platformRebootProtocol(ScriptProperties scriptProperties) {
        return new PlatformRebootProtocol(scriptProperties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PlatformShutdownProtocol platformShutdownProtocol(ScriptProperties scriptProperties) {
        return new PlatformShutdownProtocol(scriptProperties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PlatformScriptProtocol platformScriptProtocol() {
        return new PlatformScriptProtocol();
    }
    
}
