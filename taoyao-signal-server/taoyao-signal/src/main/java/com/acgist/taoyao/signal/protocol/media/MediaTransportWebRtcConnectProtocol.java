package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 连接WebRTC通道信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        """
    },
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaTransportWebRtcConnectProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::webrtc::connect";
    
    public MediaTransportWebRtcConnectProtocol() {
        super("连接WebRTC通道信令", SIGNAL);
    }

    @Override
    public void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message) {
    }

    @Override
    public void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message) {
        final Message response = room.request(message);
        client.push(response);
    }
    
}
