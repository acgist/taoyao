package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 路由RTP能力信令
 * 
 * @author acgist
 */
@Protocol
public class RouterRtpCapabilitiesProtocol extends ProtocolRoomAdapter {

	public static final String SIGNAL = "router::rtp::capabilities";
	
	public RouterRtpCapabilitiesProtocol() {
		super("路由RTP能力信令", SIGNAL);
	}

	@Override
	public void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message) {
		// 忽略
	}

	@Override
	public void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message) {
		client.push(room.sendSync(message));
	}

}
