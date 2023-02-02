package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.media.MediaAnswerEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * Answer信令
 * 
 * @author acgist
 */
@Protocol
public class MediaAnswerProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 5998;
	
	public MediaAnswerProtocol() {
		super(PID, "Answer信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MediaAnswerEvent(sn, body, message, session));
	}

}