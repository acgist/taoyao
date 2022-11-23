package com.acgist.taoyao.signal.event.meeting;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 创建会议事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MeetingCreateEvent extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	public MeetingCreateEvent(String sn, Map<?, ?> body, Message message, ClientSession session) {
		super(sn, body, message, session);
	}

}
