package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.room.RoomEnterEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 进入房间信令
 * 
 * @author acgist
 */
@Protocol
public class RoomEnterProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "room::enter";
	
	public RoomEnterProtocol() {
		super("进入房间信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		this.publishEvent(new RoomEnterEvent(body, message, session));
	}

}
