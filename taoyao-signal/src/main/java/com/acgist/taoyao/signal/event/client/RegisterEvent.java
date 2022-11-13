package com.acgist.taoyao.signal.event.client;

import java.util.Map;

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
	/**
	 * 参数
	 */
	private Map<?, ?> data;
	
	public RegisterEvent(ClientSession session, Map<?, ?> data) {
		super(session);
		this.session = session;
		this.data = data;
	}

}
