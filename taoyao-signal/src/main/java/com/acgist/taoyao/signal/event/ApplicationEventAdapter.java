package com.acgist.taoyao.signal.event;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;

import lombok.Getter;
import lombok.Setter;

/**
 * 事件适配器
 * 
 * @author acgist
 */
@Getter
@Setter
public abstract class ApplicationEventAdapter extends ApplicationEvent {
	
	private static final long serialVersionUID = 1L;

	/**
	 * 终端标识
	 */
	private String sn;
	/**
	 * 主体
	 */
	private final Map<?, ?> body;
	/**
	 * 消息
	 */
	private final Message message;
	/**
	 * 会话
	 */
	private final ClientSession session;
	
	public ApplicationEventAdapter(Message message, ClientSession session) {
		this(session.sn(), null, message, session);
	}
	
	public ApplicationEventAdapter(Map<?, ?> body, Message message, ClientSession session) {
		this(session.sn(), body, message, session);
	}
	
	public ApplicationEventAdapter(String sn, Map<?, ?> body, Message message, ClientSession session) {
		super(session);
		this.sn = sn;
		this.body = body;
		this.message = message;
		this.session = session;
	}

}
