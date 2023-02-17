package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭媒体服务信令
 * 
 * @author acgist
 */
@Slf4j
public class MediaShutdownProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "media::shutdown";
    
    @Autowired
    private ScriptProperties scriptProperties;
    
    public MediaShutdownProtocol() {
        super("关闭媒体服务信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
        log.info("关闭媒体服务：{}", clientId);
        this.clientManager.broadcast(message);
        ScriptUtils.execute(this.scriptProperties.getMediaShutdown());        
    }

}
