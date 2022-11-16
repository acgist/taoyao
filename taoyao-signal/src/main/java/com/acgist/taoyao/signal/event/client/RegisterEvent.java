package com.acgist.taoyao.signal.event.client;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 注册事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class RegisterEvent extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	public RegisterEvent(Map<?, ?> body, Message message, ClientSession session) {
		super(body, message, session);
	}

}
