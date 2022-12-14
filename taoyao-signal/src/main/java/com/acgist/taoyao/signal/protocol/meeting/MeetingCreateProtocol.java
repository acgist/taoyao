package com.acgist.taoyao.signal.protocol.meeting;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.meeting.MeetingCreateEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 创建会议信令
 * 
 * @author acgist
 */
@Protocol
public class MeetingCreateProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 4000;
	
	public MeetingCreateProtocol() {
		super(PID, "创建会议信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MeetingCreateEvent(sn, body, message, session));
	}

}
