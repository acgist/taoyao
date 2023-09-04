package com.acgist.taoyao.signal.protocol.platform;

import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.config.ScriptProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭平台信令
 * 
 * @author acgist
 */
@Slf4j
@Description(
    flow = {
        "信令服务+)终端",
        "终端=>信令服务",
        "终端->信令服务+)终端"
    }
)
public class PlatformShutdownProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "platform::shutdown";
    
    private final ScriptProperties scriptProperties;
    
    public PlatformShutdownProtocol(ScriptProperties scriptProperties) {
        super("关闭平台信令", SIGNAL);
        this.scriptProperties = scriptProperties;
    }

    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        log.info("关闭平台：{}", clientId);
        this.shutdown(message);
    }
    
    /**
     * 关闭平台
     */
    public void execute() {
        log.info("关闭平台");
        this.shutdown(this.build());
    }
    
    /**
     * 关闭平台
     * 
     * @param message 消息
     */
    private void shutdown(Message message) {
        this.clientManager.broadcast(message);
        if(this.applicationContext instanceof ConfigurableApplicationContext context) {
            // API关闭
            if(context.isActive()) {
                // 如果需要广播完成可以设置延时
                context.close();
            } else {
                // 其他情况
            }
        } else {
            // 命令关闭
            ScriptUtils.execute(this.scriptProperties.getPlatformShutdown());
        }
    }

}
