package com.acgist.taoyao.signal.session.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.system.ErrorProtocol;
import com.acgist.taoyao.signal.session.ClientSessionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket信令
 * 
 * @author acgist
 */
@Slf4j
@ServerEndpoint(value = "/websocket.signal")
public class WebSocketSignal {
	
	private static ErrorProtocol errorProtocol;
	private static ProtocolManager protocolManager;
	private static ClientSessionManager clientSessionManager;
	
	@OnOpen
	public void open(Session session) {
		log.debug("会话连接：{}", session);
		WebSocketSignal.clientSessionManager.open(new WebSocketSession(session));
	}
	
	@OnMessage
	public void message(Session session, String message) {
		log.debug("会话消息：{}-{}", session, message);
		try {
			WebSocketSignal.protocolManager.execute(message, session);
		} catch (Exception e) {
			log.error("处理会话消息异常", e);
			final Message errorMessage = WebSocketSignal.errorProtocol.build();
			errorMessage.setBody(e.getMessage());
			this.push(session, errorMessage);
		}
	}
	
	@OnClose
	public void close(Session session) {
		log.debug("会话关闭：{}", session);
		WebSocketSignal.clientSessionManager.close(session);
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

	@Autowired
	public void setErrorProtocol(ErrorProtocol errorProtocol) {
		WebSocketSignal.errorProtocol = errorProtocol;
	}

	@Autowired
	public void setProtocolManager(ProtocolManager protocolManager) {
		WebSocketSignal.protocolManager = protocolManager;
	}
	
	@Autowired
	public void setClientSessionManager(ClientSessionManager clientSessionManager) {
		WebSocketSignal.clientSessionManager = clientSessionManager;
	}
	
}
