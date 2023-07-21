package com.acgist.taoyao.signal.client.websocket;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientAdapter;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket终端
 * 
 * @author acgist
 */
@Slf4j
public class WebSocketClient extends ClientAdapter<Session> {

    public WebSocketClient(long timeout, Session instance) {
        super(timeout, instance);
    }
    
    @Override
    public void push(Message message) {
        synchronized (this.instance) {
            try {
                if(this.instance.isOpen()) {
                    this.instance.getBasicRemote().sendText(message.toString(), true);
                } else {
                    log.error("WebSocket终端已经关闭：{}", this.instance);
                }
            } catch (Exception e) {
                log.error("WebSocket终端发送消息异常：{}", message, e);
            }
        }
    }
    
    @Override
    protected String getClientIP(Session instance) {
        return (String) instance.getUserProperties().get(Constant.IP);
    }

}
