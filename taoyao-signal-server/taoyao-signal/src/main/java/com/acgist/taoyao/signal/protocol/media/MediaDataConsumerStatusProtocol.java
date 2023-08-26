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
 * 查询数据消费者状态信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId"    : "房间ID",
        "consumerId": "数据消费者ID"
    }
    """,
    flow = "终端=>信令服务->媒体服务"
)
public class MediaDataConsumerStatusProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::data::consumer::status";
    
    public MediaDataConsumerStatusProtocol() {
        super("查询数据消费者状态信令", SIGNAL);
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
