package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.Constant;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Peer;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.media.Transport;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 创建WebRTC通道信令
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
public class MediaTransportWebRtcCreateProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::webrtc::create";
    
    protected MediaTransportWebRtcCreateProtocol() {
        super("创建WebRTC通道信令", SIGNAL);
    }

    @Override
    public void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message) {
    }

    @Override
    public void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message) {
        final Message response = room.request(message);
        final Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        final Peer peer = client.peer();
        final Map<String, Transport> transports = peer.getTransports();
        final String transportId = this.get(responseBody, Constant.TRANSPORT_ID);
        final Transport transport = transports.computeIfAbsent(transportId, key -> new Transport(client));
        transport.copy(responseBody);
        client.push(response);
    }

}
