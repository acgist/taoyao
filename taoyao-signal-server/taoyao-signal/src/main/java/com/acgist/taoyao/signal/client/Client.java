package com.acgist.taoyao.signal.client;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.mediasoup.MediasoupClient;

/**
 * 终端会话
 * 
 * @author acgist
 *
 * @param <T> 会话类型
 */
public interface Client extends AutoCloseable {

	/**
	 * @return 终端标识
	 */
	String sn();
	
	/**
	 * @return IP
	 */
	String ip();
	
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
	 * 推送消息
	 * 
	 * @param sn 终端标识
	 * @param message 消息
	 */
	void push(String sn, Message message);

	/**
	 * @param timeout 超时时间
	 * 
	 * @return 是否超时会话
	 */
	boolean timeout(long timeout);
	
	/**
	 * @return 终端实例
	 */
	AutoCloseable instance();
	
	/**
	 * 设置授权
	 * 
	 * @param sn 重点标识
	 */
	void authorize(String sn);
	
	/**
	 * @return 是否授权
	 */
	boolean authorized();
	
	/**
	 * @return Mediasoup终端
	 */
	MediasoupClient mediasoupClient();
	
	/**
	 * @param mediasoupClient Mediasoup终端
	 */
	void mediasoupClient(MediasoupClient mediasoupClient);
	
}
