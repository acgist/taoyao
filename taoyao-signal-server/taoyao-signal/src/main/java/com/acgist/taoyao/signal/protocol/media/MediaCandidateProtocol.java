package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.media.MediaCandidateEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 候选信令
 * 
 * @author acgist
 */
@Protocol
public class MediaCandidateProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 5999;
	
	public MediaCandidateProtocol() {
		super(PID, "候选信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MediaCandidateEvent(sn, body, message, session));
	}

}