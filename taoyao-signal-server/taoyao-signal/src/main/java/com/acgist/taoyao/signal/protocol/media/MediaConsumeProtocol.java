package com.acgist.taoyao.signal.protocol.media;

import java.util.HashMap;
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
import com.acgist.taoyao.signal.event.MediaProduceEvent;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Consumer;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.party.media.Transport;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 消费媒体信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    flow = {
        "信令服务->媒体服务=>信令服务=>终端->信令服务->信令服务",
        "终端->信令服务->媒体服务=>信令服务=>终端->信令服务->信令服务"
    }
)
public class MediaConsumeProtocol extends ProtocolRoomAdapter implements ApplicationListener<MediaProduceEvent> {

    public static final String SIGNAL = "media::consume";
    
    public MediaConsumeProtocol() {
        super("消费媒体信令", SIGNAL);
    }
    
    @Async
    @Override
    public void onApplicationEvent(MediaProduceEvent event) {
        final Room room = event.getRoom();
        final Producer producer = event.getProducer();
        final ClientWrapper clientWrapper = producer.getProduceClient();
        final Client client = clientWrapper.getClient();
        room.getClients().values().stream()
        .filter(v -> v.getClient() != client)
        .filter(v -> v.getSubscribeType().consume(producer))
        .forEach(v -> this.consume(room, v, producer));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String producerId = MapUtils.get(body, Constant.PRODUCER_ID);
        final Producer producer = room.producer(producerId);
        if(clientType == ClientType.WEB || clientType == ClientType.CAMERA) {
            // 请求消费
            this.consume(room, room.clientWrapper(client), producer);
        } else if(clientType == ClientType.MEDIA) {
            // 等待消费者准备完成
            final String kind = MapUtils.get(body, Constant.KIND);
            final String streamId = MapUtils.get(body, Constant.STREAM_ID);
            final String consumerId = MapUtils.get(body, Constant.CONSUMER_ID);
            final String consumeClientId = MapUtils.get(body, Constant.CLIENT_ID);
            final ClientWrapper consumeClientWrapper = room.clientWrapper(consumeClientId);
            final Map<String, Consumer> consumers = producer.getConsumers();
            final Consumer consumer = consumers.get(producerId);
            if(consumer != null) {
                log.warn("消费者已经存在：{}", consumerId);
                // TODO：关闭旧的
//                consumer.close();
            }
            final Client consumeClient = consumeClientWrapper.getClient();
            consumers.put(consumerId, new Consumer(consumeClientWrapper, producer, kind, streamId, consumerId));
            consumeClient.push(message);
        } else {
            // TODO：log
        }
    }
    
    /**
     * 消费媒体
     * 
     * @param room
     * @param consumeClient
     * @param producer
     */
    private void consume(Room room, ClientWrapper consumeClientWrapper, Producer producer) {
        if(producer.getProduceClient().consume(producer)) {
            log.debug("已经消费：{}", consumeClientWrapper.getClientId());
            return;
        }
        final Client mediaClient = room.getMediaClient();
        final Transport recvTransport = consumeClientWrapper.getRecvTransport();
        final Map<String, Object> body = new HashMap<>();
        final String clientId = consumeClientWrapper.getClientId();
        final String streamId = producer.getStreamId() + "->" + clientId;
        body.put(Constant.ROOM_ID, room.getRoomId());
        body.put(Constant.CLIENT_ID, clientId);
        body.put(Constant.SOURCE_ID, producer.getProduceClient().getClientId());
        body.put(Constant.STREAM_ID, streamId);
        body.put(Constant.PRODUCER_ID, producer.getProducerId());
        body.put(Constant.TRANSPORT_ID, recvTransport.getTransportId());
        body.put(Constant.RTP_CAPABILITIES, consumeClientWrapper.getRtpCapabilities());
        body.put(Constant.SCTP_CAPABILITIES, consumeClientWrapper.getSctpCapabilities());
        mediaClient.push(this.build(body));
    }

}
