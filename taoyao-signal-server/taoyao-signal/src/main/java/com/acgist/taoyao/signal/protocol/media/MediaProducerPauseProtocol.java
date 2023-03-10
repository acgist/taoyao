package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.context.ApplicationListener;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.media.MediaProducerPauseEvent;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 暂停生产者信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID"
        "producerId": "消费者ID"
    }
    """,
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaProducerPauseProtocol extends ProtocolRoomAdapter implements ApplicationListener<MediaProducerPauseEvent> {

    public static final String SIGNAL = "media::producer::pause";
    
    public MediaProducerPauseProtocol() {
        super("暂停生产者信令", SIGNAL);
    }
    
    @Override
    public void onApplicationEvent(MediaProducerPauseEvent event) {
        final Room room = event.getRoom();
        final Client mediaClient = event.getMediaClient();
        final Map<String, Object> body = Map.of(
            Constant.ROOM_ID, room.getRoomId(),
            Constant.PRODUCER_ID, event.getProducerId()
        );
        mediaClient.push(this.build(body));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.mediaClient()) {
            final String producerId = MapUtils.get(body, Constant.PRODUCER_ID);
            final Producer producer = room.producer(producerId);
            producer.pause();
        } else if(clientType.mediaServer()) {
            room.broadcast(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
