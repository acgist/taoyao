package com.acgist.taoyao.signal.event.client;

import org.springframework.context.ApplicationEvent;

import com.acgist.taoyao.signal.session.ClientSession;

import lombok.Getter;
import lombok.Setter;

/**
 * 终端注册事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class RegisterEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 会话
	 */
	private ClientSession session;
	
	public RegisterEvent(ClientSession session) {
		super(session);
		this.session = session;
	}

}
