package com.acgist.taoyao.signal.session.websocket;

import java.io.IOException;

import javax.websocket.Session;

import com.acgist.taoyao.signal.message.Message;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 会话包装器
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class SessionWrapper {

	/**
	 * 终端帐号
	 */
	private String sn;
	/**
	 * 会话
	 */
	private Session session;
	
	/**
	 * 发送消息
	 * 
	 * @param message 消息
	 */
	public void send(Message message) {
		try {
			this.session.getBasicRemote().sendText(message.toString());
		} catch (IOException e) {
			log.error("WebSocket发送消息异常：{}", message, e);
		}
	}
	
	/**
	 * @param sn 终端编号
	 * 
	 * @return 是否匹配成功
	 */
	public boolean matchSn(String sn) {
		return this.sn != null && this.sn.equals(sn);
	}
	
	/**
	 * @param sn 终端编号
	 * 
	 * @return 是否匹配失败
	 */
	public boolean matchNoneSn(String sn) {
		return this.sn != null && !this.sn.equals(sn);
	}

	/**
	 * @param session 会话
	 * 
	 * @return 是否匹配成功
	 */
	public boolean matchSession(Session session) {
		return this.session != null && this.session.equals(session);
	}

}
