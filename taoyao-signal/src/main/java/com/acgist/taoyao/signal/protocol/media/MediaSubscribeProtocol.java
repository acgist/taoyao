package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.media.MediaSubscribeEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 订阅指令
 * 
 * @author acgist
 */
public class MediaSubscribeProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 5002;
	
	public MediaSubscribeProtocol() {
		super(PID, "订阅指令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MediaSubscribeEvent(sn, body, message, session));
	}

}
