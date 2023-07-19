package com.acgist.taoyao.signal.party.session;

import java.io.Closeable;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.session.SessionCloseEvent;

import lombok.Getter;

/**
 * 视频会话
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
     * 推送消息
     * 
     * @param message 消息
     */
    public void push(Message message) {
        this.source.push(message);
        this.target.push(message);
    }
    
    /**
     * 发送消息给对方终端
     * 
     * @param clientId 当前终端ID
     * @param message  消息
     */
    public void pushOther(String clientId, Message message) {
        if(this.source.getClientId().equals(clientId)) {
            this.target.push(message);
        } else {
            this.source.push(message);
        }
    }
    
    /**
     * @param client 终端
     * 
     * @return 是否含有终端
     */
    public boolean hasClient(Client client) {
        return this.source == client || this.target == client;
    }
    
    @Override
    public void close() {
        EventPublisher.publishEvent(new SessionCloseEvent(this));
    }

}
