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
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.media.Room;
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
            "kind": "媒体类型",
            "roomId": "房间标识",
            "transportId": "通道标识",
            "rtpParameters": "rtpParameters"
        }
        """
    },
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaProduceProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::produce";
    
    public MediaProduceProtocol() {
        super("生产媒体信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.mediaClient()) {
            final String kind = MapUtils.get(body, Constant.KIND);
            final String streamId = kind + "::" + clientId;
            body.put(Constant.CLIENT_ID, clientId);
            body.put(Constant.STREAM_ID, streamId);
            final Message response = room.request(message);
            final Map<String, Object> responseBody = response.body();
            final String producerId = MapUtils.get(responseBody, Constant.PRODUCER_ID);
            final ClientWrapper producerClientWrapper = room.clientWrapper(client);
            final Map<String, Producer> roomProducers = room.getProducers();
            final Map<String, Producer> clientProducers = producerClientWrapper.getProducers();
            final Producer producer = new Producer(kind, streamId, producerId, room, producerClientWrapper);
            final Producer oldRoomProducer = roomProducers.put(producerId, producer);
            final Producer oldClientProducer = clientProducers.put(producerId, producer);
            if(oldRoomProducer != null || oldClientProducer != null) {
                log.warn("生产者已经存在：{}", producerId);
            }
            final Message responseMessage = response.cloneWithoutBody();
            responseMessage.setBody(Map.of(
                Constant.KIND, kind,
                Constant.STREAM_ID, streamId,
                Constant.PRODUCER_ID, producerId
                ));
            room.broadcast(responseMessage);
            log.info("{}生产媒体：{} - {}", clientId, streamId, producerId);
            this.publishEvent(new MediaConsumeEvent(room, producer));
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
