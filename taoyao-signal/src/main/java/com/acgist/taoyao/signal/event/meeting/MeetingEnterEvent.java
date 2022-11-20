package com.acgist.taoyao.signal.event.meeting;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

/**
 * 进入会议事件
 * 
 * @author acgist
 */
public class MeetingEnterEvent extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	public MeetingEnterEvent(Map<?, ?> body, Message message, ClientSession session) {
		super(body, message, session);
	}

}
