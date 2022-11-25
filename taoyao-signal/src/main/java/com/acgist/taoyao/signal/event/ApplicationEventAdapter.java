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
	
	/**
	 * @param <T> 参数泛型
	 * 
	 * @param key 参数名称
	 * 
	 * @return 值
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		if(this.body == null) {
			return null;
		}
		return (T) this.body.get(key);
	}

	/**
	 * @param <T> 参数泛型
	 * 
	 * @param key 参数名称
	 * @param defaultValue 默认值
	 * 
	 * @return 值
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String key, T defaultValue) {
		if(this.body == null) {
			return defaultValue;
		}
		final T t = (T) this.body.get(key);
		return t == null ? defaultValue : t;
	}

}
