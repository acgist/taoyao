package com.acgist.taoyao.signal.protocol.media;

import java.net.http.WebSocket;
import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolMediaRoomAdapter;
import com.acgist.taoyao.signal.room.Room;

/**
 * 当前讲话终端信令
 * 
 * @author acgist
 */
@Protocol
public class AudioActiveSpeakerProtocol extends ProtocolMediaRoomAdapter {

	public static final String SIGNAL = "audio::active::speaker";
	
	public AudioActiveSpeakerProtocol() {
		super("当前讲话终端信令", SIGNAL);
	}

	@Override
	public void execute(Room room, Map<?, ?> body, Message message, WebSocket webSocket) {
		room.broadcast(message);
	}
	
	@Override
	public void execute(String sn, Room room, Map<?, ?> body, Client client, Message message) {
		// 忽略
	}

}
