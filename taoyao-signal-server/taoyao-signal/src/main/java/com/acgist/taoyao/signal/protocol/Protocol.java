package com.acgist.taoyao.signal.protocol;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

/**
 * 信令
 * 
 * 1000~1999：平台信令
 * 2000~2999：终端信令
 * 3000~3999：会议信令
 * 4000~4999：直播信令
 * 5000~5999：媒体信令
 * 6000~6999：媒体信令（Mediasoup）
 * 
 * @author acgist
 */
public interface Protocol {
	
	/**
	 * @return 信令协议标识
	 */
	Integer pid();
	
	/**
	 * @return 信令名称
	 */
	String name();
	
	/**
	 * 鉴权
	 * 
	 * @param message 信令
	 * 
	 * @return 是否成功
	 */
	default boolean authenticate(Message message) {
		return true;
	}

	/**
	 * 处理信令消息
	 * 
	 * @param sn 终端标识
	 * @param message 信令消息
	 * @param session 会话
	 */
	void execute(String sn, Message message, ClientSession session);

	/**
	 * 发布事件
	 * 
	 * @param <E> 事件类型
	 * 
	 * @param event 事件
	 */
	<E extends ApplicationEventAdapter> void publishEvent(E event);
	
	/**
	 * 创建信令消息
	 * 
	 * @return 信令消息
	 */
	Message build();
	
	/**
	 * 创建信令消息
	 * 
	 * @param body 请求响应主体
	 * 
	 * @return 信令消息
	 */
	Message build(Object body);
	
	/**
	 * 创建信令消息
	 * 
	 * @param code 响应编码
	 * @param body 请求响应主体
	 * 
	 * @return 信令消息
	 */
	Message build(MessageCode code, Object body);
	
	/**
	 * 创建信令消息
	 * 
	 * @param code 响应编码
	 * @param message 响应描述
	 * @param body 请求响应主体
	 * 
	 * @return 信令消息
	 */
	Message build(MessageCode code, String message, Object body);
	
	/**
	 * 创建信令消息
	 * 
	 * @param id 请求响应标识
	 * @param code 响应编码
	 * @param message 响应描述
	 * @param body 请求响应主体
	 * 
	 * @return 信令消息
	 */
	Message build(String id, MessageCode code, String message, Object body);
	
}
