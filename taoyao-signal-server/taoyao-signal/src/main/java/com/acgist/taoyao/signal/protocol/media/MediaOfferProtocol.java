package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.media.MediaOfferEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * Offer信令
 * 
 * @author acgist
 */
@Protocol
public class MediaOfferProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 5997;
	
	public MediaOfferProtocol() {
		super(PID, "Offer信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new MediaOfferEvent(sn, body, message, session));
	}

}