package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import org.springframework.cglib.beans.BeanMap;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.BeanUtils;
import com.acgist.taoyao.signal.client.ClientSession;

/**
 * Java主体信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolJavaAdapter<T> extends ProtocolAdapter {

	/**
	 * 对象类型
	 */
	private final Class<T> clazz;
	
	protected ProtocolJavaAdapter(Integer pid, String name, Class<T> clazz) {
		super(pid, name);
		this.clazz = clazz;
	}
	
	@Override
	public void execute(String sn, Message message, ClientSession session) {
		final Object body = message.getBody();
		if(body instanceof Map<?, ?> map) {
			final T t = BeanUtils.newInstance(this.clazz);
			final BeanMap beanMap = BeanMap.create(t);
			beanMap.putAll(map);
			this.execute(sn, t, message, session);
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
	public abstract void execute(String sn, T body, Message message, ClientSession session);
	
}
