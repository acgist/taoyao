package com.acgist.taoyao.signal.client;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.media.MediaClient;

/**
 * 终端
 * 
 * @author acgist
 */
public interface Client extends AutoCloseable {

	/**
	 * @return IP
	 */
	String ip();
	
	/**
	 * @return 终端标识
	 */
	String clientId();
	
	/**
	 * @return 终端状态
	 */
	ClientStatus status();
	
	/**
	 * 推送消息
	 * 
	 * @param message 消息
	 */
	void push(Message message);
	
	/**
	 * @param timeout 超时时间
	 * 
	 * @return 授权是否超时
	 */
	boolean timeout(long timeout);
	
	/**
	 * @return 终端实例
	 */
	AutoCloseable instance();
	
	/**
	 * 设置授权
	 * 
	 * @param clientId 终端标识
	 */
	void authorize(String clientId);
	
	/**
	 * @return 是否授权
	 */
	boolean authorized();
	
	/**
	 * @return 媒体服务终端
	 */
	MediaClient mediaClient();
	
	/**
	 * @param mediaClient 媒体服务终端
	 */
	void mediaClient(MediaClient mediaClient);
	
}
