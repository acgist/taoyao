package com.acgist.taoyao.meeting.listener;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.meeting.Meeting;
import com.acgist.taoyao.meeting.MeetingListenerAdapter;
import com.acgist.taoyao.signal.event.meeting.MeetingEnterEvent;

/**
 * 进入会议监听
 * 
 * @author acgist
 */
@EventListener
public class MeetingEnterListener extends MeetingListenerAdapter<MeetingEnterEvent> {

	@Override
	public void onApplicationEvent(MeetingEnterEvent event) {
		final String sn = event.getSn();
		final String id = event.get("id");
		final Meeting meeting = this.meetingManager.meeting(id);
		if(meeting == null) {
			throw MessageCodeException.of(MessageCode.CODE_3400, "无效会议");
		}
		meeting.addSn(sn);
		final Message message = event.getMessage();
		message.setBody(Map.of(
			"id", meeting.getId(),
			"sn", sn
		));
		// TODO：返回房间列表
		meeting.getSns().stream()
		.filter(v -> !sn.equals(v))
		.forEach(v -> this.clientSessionManager.unicast(v, message));
	}

}
