package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 连接WebRTC通道信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = {
        """
        {
            "roomId"        : "房间ID",
            "transportId"   : "通道ID",
            "dtlsParameters": "DTLS参数"
        }
        {
            "roomId"        : "房间标识",
            "transportId"   : "传输通道标识"
        }
        """
    },
    flow = "终端=>信令服务->媒体服务"
)
public class MediaTransportWebRtcConnectProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::webrtc::connect";
    
    public MediaTransportWebRtcConnectProtocol() {
        super("连接WebRTC通道信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            final Message response = room.requestMedia(message);
            final Map<String, Object> responseBody = response.body();
            client.push(response);
            final String transportId = MapUtils.get(responseBody, Constant.TRANSPORT_ID);
            log.info("{}连接WebRTC通道信令：{}", clientId, transportId);
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
