package com.acgist.taoyao.signal.protocol;

import org.springframework.context.ApplicationEvent;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.session.ClientSession;

/**
 * 信令协议
 * 
 * 1000~1999：系统信令（关机）
 * 2000~2999：终端信令（注册、注销、终端列表）
 * 3000~3999：直播信令
 * 4000~4999：会议信令
 * 9999：信令异常
 * 
 * @author acgist
 */
public interface Protocol {
	
	/**
	 * @return 信令协议标识
	 */
	Integer protocol();

	/**
	 * 处理信令消息
	 * 
	 * @param sn 终端标识
	 * @param message 信令消息
	 * @param session 会话
	 * 
	 * @return 事件
	 */
	ApplicationEvent execute(String sn, Message message, ClientSession session);
	
	/**
	 * 创建信令消息
	 * 
	 * @return 信令消息
	 */
	Message build();
	
}
