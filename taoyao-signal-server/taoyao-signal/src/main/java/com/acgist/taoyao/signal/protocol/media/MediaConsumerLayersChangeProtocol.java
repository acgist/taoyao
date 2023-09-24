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
 * 消费者空间层和时间层改变信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId"       : "房间ID"
        "consumerId"   : "消费者ID",
        "spatialLayer" : 最佳空间层,
        "temporalLayer": 最佳时间层
    }
    """,
    flow = "媒体服务->信令服务+)终端"
)
public class MediaConsumerLayersChangeProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::consumer::layers::change";
    
    protected MediaConsumerLayersChangeProtocol() {
        super("消费者空间层和时间层改变信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isMedia()) {
            room.broadcast(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
