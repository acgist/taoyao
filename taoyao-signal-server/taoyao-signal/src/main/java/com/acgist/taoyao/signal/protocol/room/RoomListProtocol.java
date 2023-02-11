package com.acgist.taoyao.signal.protocol.room;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;
import com.acgist.taoyao.signal.room.RoomManager;

/**
 * 房间列表信令
 * 
 * @author acgist
 */
@Protocol
public class RoomListProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "room::list";
	
	@Autowired
	private RoomManager roomManager;
	
	public RoomListProtocol() {
		super("房间列表信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Client client, Message message) {
		message.setBody(this.roomManager.status());
		client.push(message);
	}

}
