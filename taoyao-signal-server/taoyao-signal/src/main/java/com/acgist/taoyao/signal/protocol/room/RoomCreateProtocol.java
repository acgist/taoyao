package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.room.RoomCreateEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 创建房间信令
 * 
 * @author acgist
 */
@Protocol
public class RoomCreateProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "room::create";
	
	public RoomCreateProtocol() {
		super("创建房间信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Client client, Message message) {
		this.publishEvent(new RoomCreateEvent(body, client, message));
	}

}
