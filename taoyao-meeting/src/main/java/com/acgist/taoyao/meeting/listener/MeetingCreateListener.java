package com.acgist.taoyao.meeting.listener;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.meeting.MeetingManager;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.meeting.MeetingCreateEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 创建会议监听
 * 
 * @author acgist
 */
@Slf4j
@Component
public class MeetingCreateListener extends ApplicationListenerAdapter<MeetingCreateEvent> {

	@Autowired
	private MeetingManager meetingManager;
	
	@Override
	public void onApplicationEvent(MeetingCreateEvent event) {
//		this.meetingManager.create();
		final ClientSession session = event.getSession();
		final Message message = event.getMessage();
		message.setBody(Map.of("id", "1234"));
		session.push(message);
	}

}
