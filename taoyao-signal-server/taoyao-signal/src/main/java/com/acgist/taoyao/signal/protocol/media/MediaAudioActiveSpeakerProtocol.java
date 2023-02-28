package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 当前讲话终端信令
 * 
 * @author acgist
 */
@Protocol
public class MediaAudioActiveSpeakerProtocol extends ProtocolRoomAdapter {

	public static final String SIGNAL = "media::audio::active::speaker";
	
	public MediaAudioActiveSpeakerProtocol() {
		super("当前讲话终端信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
	    if(clientType == ClientType.MEDIA) {
	        room.broadcast(message);
	    } else {
	        // 忽略其他情况
	    }
	}

}
