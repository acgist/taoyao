package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.session.ClientSession;

/**
 * 信令协议Map主体适配器
 * 
 * @author acgist
 */
public abstract class ProtocolBodyMapAdapter extends ProtocolAdapter {

	protected ProtocolBodyMapAdapter(Integer protocol) {
		super(protocol);
	}
	
	@Override
	public ApplicationEvent execute(String sn, Message message, ClientSession session) {
		final Object body = message.getBody();
		if(body instanceof Map<?, ?> map) {
			return this.execute(sn, map, message, session);
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
	 * 
	 * @return 事件
	 */
	public abstract ApplicationEvent execute(String sn, Map<?, ?> body, Message message, ClientSession session);
	
}
