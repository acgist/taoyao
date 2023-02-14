package com.acgist.taoyao.signal.event;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端事件适配器
 * 
 * @author acgist
 */
@Getter
@Setter
public abstract class ClientEventAdapter extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	/**
	 * 终端标识
	 */
	private String sn;
	/**
	 * 终端
	 */
	private final Client client;
	
	public ClientEventAdapter(Message message, Client client) {
		this(Map.of(), message, client);
	}
	
	public ClientEventAdapter(Map<?, ?> body, Message message, Client client) {
		super(body, message, client);
		this.sn = client.sn();
		this.client = client;
	}
	
}
