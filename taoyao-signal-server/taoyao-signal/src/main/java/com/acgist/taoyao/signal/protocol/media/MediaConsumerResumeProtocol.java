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
import com.acgist.taoyao.signal.event.media.MediaConsumerResumeEvent;
import com.acgist.taoyao.signal.party.media.Consumer;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 恢复消费者信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
    {
        "roomId"    : "房间ID"
        "consumerId": "消费者ID"
    }
    """,
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaConsumerResumeProtocol extends ProtocolRoomAdapter implements ApplicationListener<MediaConsumerResumeEvent> {

    public static final String SIGNAL = "media::consumer::resume";
    
    public MediaConsumerResumeProtocol() {
        super("恢复消费者信令", SIGNAL);
    }
    
    @Async
    @Override
    public void onApplicationEvent(MediaConsumerResumeEvent event) {
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
        final String consumerId = MapUtils.get(body, Constant.CONSUMER_ID);
        final Consumer consumer = room.consumer(consumerId);
        if(consumer == null) {
            log.debug("消费者无效：{} - {}", consumerId, clientType);
            return;
        }
        if(clientType.mediaClient()) {
            consumer.resume();
        } else if(clientType.mediaServer()) {
            consumer.getConsumerClient().push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
