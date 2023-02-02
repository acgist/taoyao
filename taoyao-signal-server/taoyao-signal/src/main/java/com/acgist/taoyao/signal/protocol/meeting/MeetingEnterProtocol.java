package com.acgist.taoyao.signal.protocol.meeting;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.meeting.MeetingEnterEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 进入会议信令
 * 
 * @author acgist
 */
@Protocol
public class MeetingEnterProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 4002;
	
	public MeetingEnterProtocol() {
		super(PID, "进入会议信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MeetingEnterEvent(sn, body, message, session));
	}

}
