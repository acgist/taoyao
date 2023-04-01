package com.acgist.taoyao.signal.party.session;

import java.io.Closeable;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;

/**
 * P2P会话
 * 
 * @author acgist
 */
@Getter
public class Session implements Closeable {

    /**
     * ID
     */
    private final String id;
    /**
     * 发起者
     */
    private final Client source;
    /**
     * 接收者
     */
    private final Client target;
    
    public Session(String id, Client source, Client target) {
        this.id = id;
        this.source = source;
        this.target = target;
    }
    
    /**
     * 发送消息给对方终端
     * 
     * @param clientId 当前终端ID
     * @param message  消息
     */
    public void pushRemote(String clientId, Message message) {
        if(this.source.clientId().equals(clientId)) {
            this.target.push(message);
        } else {
            this.source.push(message);
        }
    }
    
    @Override
    public void close() {

    }


}
