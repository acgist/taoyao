package com.acgist.taoyao.signal.event.client;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 关闭事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class CloseEvent extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	public CloseEvent(Message message, ClientSession session) {
		super(message, session);
	}

}
