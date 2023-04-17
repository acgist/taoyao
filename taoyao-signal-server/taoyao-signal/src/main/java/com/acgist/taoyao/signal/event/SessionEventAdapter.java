package com.acgist.taoyao.signal.event;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.party.session.Session;

import lombok.Getter;

/**
 * 视频会话事件适配器
 * 
 * @author acgist
 */
@Getter
public abstract class SessionEventAdapter extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	/**
	 * 视频会话
	 */
	private final Session session;
	/**
	 * 视频会话ID
	 */
	private final String sessionId;
	
	public SessionEventAdapter(Session session) {
	    this(session, null, null);
	}
	
	public SessionEventAdapter(Session session, Message message) {
		this(session, message, null);
	}
	
	public SessionEventAdapter(Session session, Message message, Map<String, Object> body) {
		super(session, message, body);
		this.session   = session;
		this.sessionId = session.getId();
	}
	
}
