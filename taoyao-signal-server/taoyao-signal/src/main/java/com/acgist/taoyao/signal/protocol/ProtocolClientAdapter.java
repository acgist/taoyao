package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;

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
    public void execute(Client client, Message message) {
        this.execute(client.getClientId(), client.getClientType(), client, message, message.body());
    }

    /**
     * 处理终端信令
     * 
     * @param clientId   终端ID
     * @param clientType 终端类型
     * @param client     终端
     * @param message    信令消息
     * @param body       消息主体
     */
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
    }

}
