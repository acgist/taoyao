package com.acgist.taoyao.signal.client.websocket;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket信令
 * 
 * @author acgist
 */
@Slf4j
@ServerEndpoint(value = "/websocket.signal")
public class WebSocketSignal {
	
	private static ClientManager clientManager;
	private static ProtocolManager protocolManager;
	private static PlatformErrorProtocol platformErrorProtocol;
	
	@OnOpen
	public void open(Session session) {
		log.debug("会话连接：{}", session);
		WebSocketSignal.clientManager.open(new WebSocketClient(session));
	}
	
	@OnMessage
	public void message(Session session, String message) {
		log.debug("会话消息：{}-{}", session, message);
		try {
			WebSocketSignal.protocolManager.execute(message.strip(), session);
		} catch (Exception e) {
			log.error("处理会话消息异常", e);
			final Message errorMessage = WebSocketSignal.platformErrorProtocol.build();
			if(e instanceof MessageCodeException code) {
				errorMessage.setCode(code.getCode(), code.getMessage());
			}
			errorMessage.setBody(e.getMessage());
			this.push(session, errorMessage);
		}
	}
	
	@OnClose
	public void close(Session session) {
		log.debug("会话关闭：{}", session);
		WebSocketSignal.clientManager.close(session);
	}
	
	@OnError
	public void error(Session session, Throwable e) {
		log.error("会话异常：{}", session, e);
		this.close(session);
	}
	
	/**
	 * 推送消息
	 * 
	 * @param session 会话
	 * @param message 消息
	 */
	private void push(Session session, Message message) {
		synchronized (session) {
			try {
				if(session.isOpen()) {
					session.getBasicRemote().sendText(message.toString());
				} else {
					log.error("会话已经关闭：{}", session);
				}
			} catch (Exception e) {
				log.error("推送消息异常：{}", message, e);
			}
		}
	}
	
	@Autowired
	public void setClientManager(ClientManager clientManager) {
		WebSocketSignal.clientManager = clientManager;
	}

	@Autowired
	public void setProtocolManager(ProtocolManager protocolManager) {
		WebSocketSignal.protocolManager = protocolManager;
	}
	
	@Autowired
	public void setPlatformErrorProtocol(PlatformErrorProtocol platformErrorProtocol) {
		WebSocketSignal.platformErrorProtocol = platformErrorProtocol;
	}
	
}
