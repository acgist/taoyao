package com.acgist.taoyao.signal.event;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.MapBodyGetter;
import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * 事件适配器
 * 
 * @author acgist
 */
@Getter
@Setter
public abstract class ApplicationEventAdapter extends ApplicationEvent implements MapBodyGetter {
	
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
	 * 终端
	 */
	private final Client client;
	/**
	 * 消息
	 */
	private final Message message;
	
	public ApplicationEventAdapter(Client client, Message message) {
		this(null, client, message);
	}
	
	public ApplicationEventAdapter(Map<?, ?> body, Client client, Message message) {
		super(client);
		this.sn = client.sn();
		this.body = body;
		this.client = client;
		this.message = message;
	}
	
	/**
	 * @see #get(Map, String)
	 */
	public <T> T get(String key) {
		return this.get(this.body, key);
	}

	/**
	 * @see #get(Map, String, Object)
	 */
	public <T> T get(String key, T defaultValue) {
		return this.get(body, key, defaultValue);
	}
	
	/**
	 * @see #getLong(Map, String)
	 */
	public Long getLong(String key) {
		return this.getLong(body, key);
	}
	
	/**
	 * @see #getInteger(Map, String)
	 */
	public Integer getInteger(String key) {
		return this.getInteger(body, key);
	}

}
