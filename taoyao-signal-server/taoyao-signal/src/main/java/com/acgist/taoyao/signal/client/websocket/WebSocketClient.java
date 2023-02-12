package com.acgist.taoyao.signal.client.websocket;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.WebSocketUtils;
import com.acgist.taoyao.signal.client.ClientAdapter;

import jakarta.websocket.RemoteEndpoint;
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
public class WebSocketClient extends ClientAdapter<Session> {

	/**
	 * 输出
	 */
	private RemoteEndpoint.Basic basic;
	
	public WebSocketClient(Session instance) {
		super(instance);
		this.ip = WebSocketUtils.getRemoteAddress(instance);
		this.basic = instance.getBasicRemote();
	}
	
	@Override
	public void push(Message message) {
		synchronized (this.instance) {
			try {
				if(this.instance.isOpen()) {
					this.basic.sendText(message.toString(), true);
				} else {
					log.error("会话已经关闭：{}", this.instance);
				}
			} catch (Exception e) {
				log.error("WebSocket发送消息异常：{}", message, e);
			}
		}
	}

}
