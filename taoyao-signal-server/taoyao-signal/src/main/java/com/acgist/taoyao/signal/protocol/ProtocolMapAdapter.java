package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;

/**
 * Map消息主体信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolMapAdapter extends ProtocolAdapter {

	protected ProtocolMapAdapter(String name, String signal) {
		super(name, signal);
	}
	
	@Override
	public void execute(String sn, Client client, Message message) {
		final Object body = message.getBody();
		if(body instanceof Map<?, ?> map) {
			this.execute(sn, map, client, message);
		} else if(body == null) {
			this.execute(sn, Map.of(), client, message);
		} else {
			throw MessageCodeException.of("信令主体类型错误：" + message);
		}
	}
	
	/**
	 * 处理信令消息
	 * 
	 * @param sn 终端标识
	 * @param body 消息主体
	 * @param client 终端
	 * @param message 信令消息
	 */
	public abstract void execute(String sn, Map<?, ?> body, Client client, Message message);
	
}
