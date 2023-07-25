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
 * 媒体消费者评分信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "score": "消费者RTP流得分表示传输质量：0~10",
        "producerScore": "生产者RTP流得分表示传输质量：0~10",
        "producerScores": [所有生产者RTP流得分]
    }
    """,
    flow = "媒体服务->信令服务->终端"
)
public class MediaConsumerScoreProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::consumer::score";
    
    public MediaConsumerScoreProtocol() {
        super("媒体消费者评分信令", SIGNAL);
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
