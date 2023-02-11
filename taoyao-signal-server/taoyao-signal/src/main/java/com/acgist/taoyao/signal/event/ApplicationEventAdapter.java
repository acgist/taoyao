package com.acgist.taoyao.signal.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.ApplicationEvent;

import com.acgist.taoyao.boot.model.Message;
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
	
	/**
	 * @param key 参数名称
	 * 
	 * @return 值
	 */
	public Long getLong(String key) {
		if(this.body == null) {
			return null;
		}
		final Object object = this.body.get(key);
		if(object == null) {
			return null;
		} else if(object instanceof Long value) {
			return value;
		}
		return Long.valueOf(object.toString());
	}

	/**
	 * @return 新的主体
	 */
	public Map<String, Object> mergeBody() {
		final Map<String, Object> body = new HashMap<>();
		if(this.body != null) {
			this.body.forEach((k, v) -> body.put(Objects.toString(k), v));
		}
		this.message.setBody(body);
		return body;
	}

}
