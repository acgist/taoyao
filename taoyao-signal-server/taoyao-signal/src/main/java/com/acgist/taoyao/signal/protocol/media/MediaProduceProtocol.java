package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.media.MediaConsumeEvent;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.room.ClientWrapper;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 生产媒体信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = {
        """
        {
            "kind"         : "媒体类型",
            "roomId"       : "房间ID",
            "transportId"  : "通道ID",
            "rtpParameters": "rtpParameters"
        }
        """,
        """
        {
            "kind"      : "媒体类型",
            "roomId"    : "房间ID",
            "producerId": "生产者ID",
        }
        """
    },
    flow = "终端=>信令服务->媒体服务"
)
public class MediaProduceProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::produce";
    
    public MediaProduceProtocol() {
        super("生产媒体信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            final String kind     = MapUtils.get(body, Constant.KIND);
            final String streamId = Constant.STREAM_ID_PRODUCER.apply(kind, clientId);
            body.put(Constant.CLIENT_ID, clientId);
            body.put(Constant.STREAM_ID, streamId);
            final Message response = room.requestMedia(message);
            final Map<String, Object> responseBody = response.body();
            final String producerId = MapUtils.get(responseBody, Constant.PRODUCER_ID);
            final ClientWrapper producerClientWrapper   = room.clientWrapper(client);
            final Map<String, Producer> roomProducers   = room.getProducers();
            final Map<String, Producer> clientProducers = producerClientWrapper.getProducers();
            final Producer producer          = new Producer(kind, streamId, producerId, room, producerClientWrapper);
            final Producer oldRoomProducer   = roomProducers.put(producerId, producer);
            final Producer oldClientProducer = clientProducers.put(producerId, producer);
            if(oldRoomProducer != null || oldClientProducer != null) {
                log.warn("生产者已经存在：{}", producerId);
            }
            final Message responseMessage = response.cloneWithoutBody();
            responseMessage.setBody(Map.of(
                Constant.KIND,        kind,
                Constant.STREAM_ID,   streamId,
                Constant.PRODUCER_ID, producerId
            ));
            client.push(responseMessage);
            log.info("{}生产媒体：{} - {}", clientId, streamId, producerId);
            this.publishEvent(new MediaConsumeEvent(room, producer));
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
