package com.acgist.taoyao.meeting.listener;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.meeting.Meeting;
import com.acgist.taoyao.meeting.MeetingManager;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionManager;
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
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	@Override
	public void onApplicationEvent(MeetingCreateEvent event) {
		final ClientSession session = event.getSession();
		final Meeting meeting = this.meetingManager.create(session.sn());
		final Message message = event.getMessage();
		message.setBody(Map.of("id", meeting.getId()));
		// 广播：不改ID触发创建终端事件回调
		this.clientSessionManager.broadcast(message);
	}

}
