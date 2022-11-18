package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.ClientSession;

/**
 * Map主体信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolMapAdapter extends ProtocolAdapter {

	protected ProtocolMapAdapter(Integer pid, String name) {
		super(pid, name);
	}
	
	@Override
	public void execute(String sn, Message message, ClientSession session) {
		final Object body = message.getBody();
		if(body instanceof Map<?, ?> map) {
			this.execute(sn, map, message, session);
		} else if(body == null) {
			this.execute(sn, Map.of(), message, session);
		} else {
			throw MessageCodeException.of("信令主体类型错误：" + message);
		}
	}
	
	/**
	 * 处理信令消息
	 * 
	 * @param sn 终端标识
	 * @param body 消息主体
	 * @param message 信令消息
	 * @param session 会话
	 */
	public abstract void execute(String sn, Map<?, ?> body, Message message, ClientSession session);
	
}
