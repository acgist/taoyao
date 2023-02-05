package com.acgist.taoyao.media.meeting.listener;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.media.meeting.Meeting;
import com.acgist.taoyao.media.meeting.MeetingListenerAdapter;
import com.acgist.taoyao.signal.event.meeting.MeetingCreateEvent;

/**
 * 创建会议监听
 * 
 * @author acgist
 */
@EventListener
public class MeetingCreateListener extends MeetingListenerAdapter<MeetingCreateEvent> {

	@Override
	public void onApplicationEvent(MeetingCreateEvent event) {
		final Meeting meeting = this.meetingManager.create(event.getSn());
		final Message message = event.getMessage();
		message.setBody(Map.of("id", meeting.getId()));
		this.clientSessionManager.broadcast(message);
	}

}
