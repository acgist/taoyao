package com.acgist.taoyao.signal.client.websocket;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSessionAdapter;

import jakarta.websocket.Session;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket会话
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class WebSocketSession extends ClientSessionAdapter<Session> {

	public WebSocketSession(Session instance) {
		super(instance);
	}

	@Override
	public void push(Message message) {
		synchronized (this.instance) {
			try {
				if(this.instance.isOpen()) {
					this.instance.getBasicRemote().sendText(message.toString());
				} else {
					log.error("会话已经关闭：{}", this.instance);
				}
			} catch (Exception e) {
				log.error("WebSocket发送消息异常：{}", message, e);
			}
		}
	}

}
