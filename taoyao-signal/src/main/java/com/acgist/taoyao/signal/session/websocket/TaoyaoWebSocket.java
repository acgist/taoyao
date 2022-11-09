package com.acgist.taoyao.signal.session.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.session.SessionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket信令
 * 
 * @author acgist
 */
@Slf4j
@ServerEndpoint(value = "/taoyao/websocket")
public class TaoyaoWebSocket {
	
	private ProtocolManager eventManager;
	private SessionManager sessionManager;
	
	public TaoyaoWebSocket(ProtocolManager eventManager, SessionManager sessionManager) {
		this.eventManager = eventManager;
		this.sessionManager = sessionManager;
	}

	@OnOpen
	public void open(Session session) {
		log.debug("会话连接：{}", session);
		this.sessionManager.open(session);
	}
	
	@OnMessage
	public void message(Session session, String message) {
		log.debug("会话消息：{}-{}", session, message);
		try {
			this.eventManager.execute(session, message);
		} catch (Exception e) {
			log.error("处理会话消息异常", e);
		}
	}
	
	@OnClose
	public void close(Session session) {
		log.debug("会话关闭：{}", session);
		this.sessionManager.close(session);
	}
	
	@OnError
	public void error(Session session, Throwable e) {
		log.error("会话异常：{}", session, e);
		this.close(session);
	}
	
}
