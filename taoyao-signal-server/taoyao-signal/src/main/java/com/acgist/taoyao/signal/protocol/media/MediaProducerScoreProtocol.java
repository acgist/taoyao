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
 * 媒体生产者评分信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId"    : "房间ID"
        "consumerId": "消费者ID"
        "score"     : {
            ...生产者评分
        }
    }
    """,
    flow = "媒体服务->信令服务+)终端"
)
public class MediaProducerScoreProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::producer::score";
    
    public MediaProducerScoreProtocol() {
        super("媒体生产者评分信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.mediaServer()) {
            room.broadcast(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
