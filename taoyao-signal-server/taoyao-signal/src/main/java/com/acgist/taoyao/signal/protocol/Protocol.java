package com.acgist.taoyao.signal.protocol;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

/**
 * 信令
 * 
 * room::     房间信令
 * media::    媒体信令
 * client::   终端信令
 * system::   系统信令
 * control::  控制信令
 * platform:: 平台信令
 * 
 * @author acgist
 */
public interface Protocol {
	
	/**
	 * @return 信令名称
	 */
	String name();
	
	/**
	 * @return 信令标识
	 */
	String signal();
	
	/**
	 * 鉴权
	 * 
	 * @param message 信令消息
	 * 
	 * @return 是否成功
	 */
	default boolean authenticate(Message message) {
		return true;
	}
	
	/**
	 * 处理终端信令
	 * 
	 * @param client 终端
	 * @param message 信令消息
	 */
	void execute(Client client, Message message);

	/**
	 * 发布事件
	 * 
	 * @param <E> 事件类型
	 * 
	 * @param event 事件
	 */
	<E extends ApplicationEventAdapter> void publishEvent(E event);
	
	/**
	 * @return 信令消息
	 */
	Message build();
	
	/**
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(Object body);
	
	/**
	 * @param messageCode 状态编码
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(MessageCode messageCode, Object body);
	
	/**
	 * @param message 状态描述
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(String message, Object body);
	
	/**
	 * @param messageCode 状态编码
	 * @param message 状态描述
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(MessageCode messageCode, String message, Object body);
	
	/**
	 * @param id 消息标识
	 * @param messageCode 状态编码
	 * @param message 状态描述
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(Long id, MessageCode messageCode, String message, Object body);
	
}
