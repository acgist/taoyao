package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.media.MediaConsumerPauseEvent;
import com.acgist.taoyao.signal.party.media.Consumer;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 暂停消费者信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID"
        "consumerId": "消费者ID"
    }
    """,
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaConsumerPauseProtocol extends ProtocolRoomAdapter implements ApplicationListener<MediaConsumerPauseEvent> {

    public static final String SIGNAL = "media::consumer::pause";
    
    public MediaConsumerPauseProtocol() {
        super("暂停消费者信令", SIGNAL);
    }
    
    @Async
    @Override
    public void onApplicationEvent(MediaConsumerPauseEvent event) {
        final Room room = event.getRoom();
        final Client mediaClient = event.getMediaClient();
        final Map<String, Object> body = Map.of(
            Constant.ROOM_ID, room.getRoomId(),
            Constant.CONSUMER_ID, event.getConsumerId()
        );
        mediaClient.push(this.build(body));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.mediaClient()) {
            final String consumerId = MapUtils.get(body, Constant.CONSUMER_ID);
            final Consumer consumer = room.consumer(consumerId);
            consumer.pause();
        } else if(clientType.mediaServer()) {
            // TODO：路由到真实消费者
            room.broadcast(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
