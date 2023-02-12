package com.acgist.taoyao.signal.service;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.Protocol;

/**
 * 信令安全
 * 
 * @author acgist
 */
public interface SecurityService {

	/**
	 * 鉴权
	 * 
	 * @param username 用户名称
	 * @param password 用户密码
	 * 
	 * @return 是否成功
	 */
	boolean authenticate(String username, String password);
	
	/**
	 * 鉴权
	 * 
	 * @param message 信令
	 * @param client 会话
	 * @param protocol 协议
	 * 
	 * @return 是否成功
	 */
	boolean authenticate(Message message, Client client, Protocol protocol);
	
}
