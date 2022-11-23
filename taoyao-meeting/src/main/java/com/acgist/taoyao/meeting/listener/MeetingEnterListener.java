package com.acgist.taoyao.meeting.listener;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.meeting.Meeting;
import com.acgist.taoyao.meeting.MeetingManager;
import com.acgist.taoyao.signal.event.meeting.MeetingEnterEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

/**
 * 进入会议监听
 * 
 * @author acgist
 */
@Component
public class MeetingEnterListener extends ApplicationListenerAdapter<MeetingEnterEvent> {

	@Autowired
	private MeetingManager meetingManager;
	
	@Override
	public void onApplicationEvent(MeetingEnterEvent event) {
		final String sn = event.getSn();
		final Map<?, ?> body = event.getBody();
		final String id = (String) body.get("id");
		final Meeting meeting = this.meetingManager.meeting(id);
		meeting.addSn(sn);
		final Message message = event.getMessage();
		message.setBody(Map.of(
			"id", meeting.getId(),
			"sn", sn
		));
		this.clientSessionManager.broadcast(sn, message);
	}

}
