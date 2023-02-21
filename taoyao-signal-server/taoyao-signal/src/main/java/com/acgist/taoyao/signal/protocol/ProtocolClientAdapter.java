package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;

/**
 * 终端信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolClientAdapter extends ProtocolAdapter {

    protected ProtocolClientAdapter(String name, String signal) {
        super(name, signal);
    }
    
    @Override
    public void execute(MediaClient mediaClient, Message message) {
    }

    @Override
    public void execute(Client client, Message message) {
        final Object body = message.getBody();
        if(body instanceof Map<?, ?> map) {
            this.execute(client.clientId(), map, client, message);
        } else if(body == null) {
            this.execute(client.clientId(), Map.of(), client, message);
        } else {
            throw MessageCodeException.of("信令主体类型错误：" + message);
        }
    }

    /**
     * 处理终端信令
     * 
     * @param clientId 终端标识
     * @param body 消息主体
     * @param client 终端
     * @param message 信令消息
     */
    public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
    }

}
