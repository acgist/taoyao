package com.acgist.taoyao.signal.protocol;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

/**
 * 信令协议
 * 
 * room::     房间信令
 * media::    媒体信令
 * client::   终端信令
 * system::   系统信令
 * platform:: 平台信令
 * 
 * @author acgist
 */
public interface Protocol {
	
	/**
	 * @return 信令协议名称
	 */
	String name();
	
	/**
	 * @return 信令协议标识
	 */
	String signal();
	
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
	 * @param client 终端
	 * @param message 信令消息
	 */
	void execute(String sn, Client client, Message message);

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
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(Object body);
	
	/**
	 * 创建信令消息
	 * 
	 * @param code 状态编码
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(MessageCode code, Object body);
	
	/**
	 * 创建信令消息
	 * 
	 * @param code 状态编码
	 * @param message 状态描述
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(String message, Object body);
	
	/**
	 * 创建信令消息
	 * 
	 * @param code 状态编码
	 * @param message 状态描述
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(MessageCode code, String message, Object body);
	
	/**
	 * 创建信令消息
	 * 
	 * @param id 消息标识
	 * @param code 状态编码
	 * @param message 状态描述
	 * @param body 消息主体
	 * 
	 * @return 信令消息
	 */
	Message build(String id, MessageCode code, String message, Object body);
	
}
