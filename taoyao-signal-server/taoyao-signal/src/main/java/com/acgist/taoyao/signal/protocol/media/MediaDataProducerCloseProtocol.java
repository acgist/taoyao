package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.media.MediaDataProducerCloseEvent;
import com.acgist.taoyao.signal.party.media.DataProducer;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭数据生产者信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    memo = "关闭通过回调实现所以不能同步响应，以下场景出现：主动断开、离开房间、信令断开",
    body = """
    {
        "roomId"    : "房间ID"
        "consumerId": "数据生产者ID"
    }
    """,
    flow = {
        "媒体服务->信令服务->终端",
        "信令服务->媒体服务->信令服务->终端",
        "终端->信令服务->媒体服务->信令服务->终端"
    }
)
public class MediaDataProducerCloseProtocol extends ProtocolRoomAdapter implements ApplicationListener<MediaDataProducerCloseEvent> {

    public static final String SIGNAL = "media::data::producer::close";
    
    public MediaDataProducerCloseProtocol() {
        super("关闭数据生产者信令", SIGNAL);
    }
    
    @Async
    @Override
    public void onApplicationEvent(@NonNull MediaDataProducerCloseEvent event) {
        final Room room = event.getRoom();
        final Client mediaClient = event.getMediaClient();
        final Map<String, Object> body = Map.of(
            Constant.ROOM_ID,     room.getRoomId(),
            Constant.PRODUCER_ID, event.getProducerId()
        );
        mediaClient.push(this.build(body));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String producerId = MapUtils.get(body, Constant.PRODUCER_ID);
        final DataProducer dataProducer = room.dataProducer(producerId);
        if(dataProducer == null) {
            log.debug("数据生产者无效：{} - {}", producerId, clientType);
            return;
        }
        if(clientType.isClient()) {
            dataProducer.close();
        } else if(clientType.isMedia()) {
            dataProducer.remove();
            dataProducer.getProducerClient().push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
