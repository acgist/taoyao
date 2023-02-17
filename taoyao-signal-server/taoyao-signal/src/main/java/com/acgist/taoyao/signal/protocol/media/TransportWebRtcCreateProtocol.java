package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 创建WebRTC通道信令
 * 
 * @author acgist
 */
@Protocol
public class TransportWebRtcCreateProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "transport:webrtc::create";
    
    protected TransportWebRtcCreateProtocol() {
        super("创建WebRTC通道信令", SIGNAL);
    }

    @Override
    public void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message) {
    }

    @Override
    public void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message) {
        room.send(message);
    }

}
