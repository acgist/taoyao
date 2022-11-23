package com.acgist.taoyao.meeting.listener;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.meeting.Meeting;
import com.acgist.taoyao.meeting.MeetingManager;
import com.acgist.taoyao.signal.event.meeting.MeetingCreateEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

/**
 * 创建会议监听
 * 
 * @author acgist
 */
@Component
public class MeetingCreateListener extends ApplicationListenerAdapter<MeetingCreateEvent> {

	@Autowired
	private MeetingManager meetingManager;
	
	@Override
	public void onApplicationEvent(MeetingCreateEvent event) {
		final Meeting meeting = this.meetingManager.create(event.getSn());
		final Message message = event.getMessage();
		message.setBody(Map.of("id", meeting.getId()));
		this.clientSessionManager.broadcast(message);
	}

}
