package com.acgist.taoyao.signal.protocol.meeting;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.meeting.MeetingCreateEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 创建会议信令
 * 
 * @author acgist
 */
@Component
public class MeetingCreateProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 4000;
	
	public MeetingCreateProtocol() {
		super(PID, "创建会议信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MeetingCreateEvent(body, message, session));
	}

}
