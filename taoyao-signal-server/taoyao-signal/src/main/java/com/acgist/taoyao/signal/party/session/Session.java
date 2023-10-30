package com.acgist.taoyao.signal.party.session;

import java.io.Closeable;
import java.util.Objects;

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
        this.id     = id;
        this.source = source;
        this.target = target;
    }
    
    /**
     * 验证权限：只有会话终端才能使用信令
     * 
     * @param client 终端
     * 
     * @return 是否通过
     */
    public boolean authenticate(Client client) {
        return this.hasClient(client);
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
    public void pushRemote(String clientId, Message message) {
        if(Objects.equals(clientId, this.source.getClientId())) {
            this.target.push(message);
        } else if(Objects.equals(clientId, this.target.getClientId())) {
            this.source.push(message);
        } else {
            // 不会出现
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
    
    /**
     * @return 来源终端ID
     */
    public String getSourceClientId() {
        return this.source == null ? null : this.source.getClientId();
    }
    
    /**
     * @return 目标终端ID
     */
    public String getTargetClientId() {
        return this.target == null ? null : this.target.getClientId();
    }
    
    @Override
    public void close() {
        EventPublisher.publishEvent(new SessionCloseEvent(this));
    }

}
