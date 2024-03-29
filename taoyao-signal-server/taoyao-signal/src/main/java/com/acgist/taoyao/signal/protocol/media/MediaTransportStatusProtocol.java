package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 查询通道状态信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId"     : "房间ID",
        "transportId": "通道ID"
    }
    {
        "roomId"     : "房间ID",
        "transportId": "通道ID",
        "status"     : [
            ...状态信息
        ]
    }
    """,
    flow = "终端=>信令服务->媒体服务"
)
public class MediaTransportStatusProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::status";
    
    public MediaTransportStatusProtocol() {
        super("查询通道状态信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            client.push(mediaClient.request(message));
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
