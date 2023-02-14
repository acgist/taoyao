package com.acgist.taoyao.signal.client.websocket;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.WebSocketUtils;
import com.acgist.taoyao.signal.client.ClientAdapter;

import jakarta.websocket.Session;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket终端
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class WebSocketClient extends ClientAdapter<Session> {

	public WebSocketClient(Session instance) {
		super(instance);
		this.ip = WebSocketUtils.getRemoteAddress(instance);
	}
	
	@Override
	public void push(Message message) {
		synchronized (this.instance) {
			try {
				if(this.instance.isOpen()) {
					this.instance.getBasicRemote().sendText(message.toString(), true);
				} else {
					log.error("WebSocket信令已经关闭：{}", this.instance);
				}
			} catch (Exception e) {
				log.error("WebSocket信令发送消息异常：{}", message, e);
			}
		}
	}

}
