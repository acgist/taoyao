package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.media.MediaPublishEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 发布信令
 * 
 * @author acgist
 */
@Protocol
public class MediaPublishProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 5000;
	
	public MediaPublishProtocol() {
		super(PID, "发布信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MediaPublishEvent(sn, body, message, session));
	}

}