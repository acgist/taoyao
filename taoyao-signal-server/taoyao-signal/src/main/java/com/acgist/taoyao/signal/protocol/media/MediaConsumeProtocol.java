package com.acgist.taoyao.signal.protocol.media;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
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
    memo = """
    消费媒体：主动消费、终端生成媒体、终端创建WebRTC消费通道
    终端生产媒体当前房间所有终端根据订阅类型自动消费媒体
    终端创建WebRTC消费通道根据订阅类型自动消费当前房间已有媒体
    """,
    flow = {
        "终端-[生产媒体]>信令服务-[其他终端消费])信令服务",
        "终端-[创建WebRTC消费通道]>信令服务-[消费其他终端])信令服务",
        "终端->信令服务->媒体服务=>信令服务->终端->信令服务->媒体服务"
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
        if(event.getProducer() != null) {
            // 生产媒体：其他终端消费
            final Producer producer = event.getProducer();
            final ClientWrapper produceClientWrapper = producer.getProduceClient();
            room.getClients().values().stream()
            .filter(v -> v != produceClientWrapper)
            .filter(v -> v.getSubscribeType().consume(producer))
            .forEach(v -> this.consume(room, v, producer));
        } else if(event.getClientWrapper() != null) {
            // 创建WebRTC消费通道：消费其他终端
            final ClientWrapper consumeClientWrapper = event.getClientWrapper();
            room.getClients().values().stream()
            .filter(v -> v != consumeClientWrapper)
            .flatMap(v -> v.getProducers().values().stream())
            .filter(v -> consumeClientWrapper.getSubscribeType().consume(v))
            .forEach(producer -> this.consume(room, consumeClientWrapper, producer));
        } else {
            throw MessageCodeException.of("消费媒体失败");
        }
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String producerId = MapUtils.get(body, Constant.PRODUCER_ID);
        final Producer producer = room.producer(producerId);
        if(clientType == ClientType.WEB || clientType == ClientType.CAMERA) {
            // 请求消费媒体
            this.consume(room, room.clientWrapper(client), producer);
        } else if(clientType == ClientType.MEDIA) {
            // 通知终端准备：准备完成再次请求消费媒体结束媒体服务等待
            final String kind = MapUtils.get(body, Constant.KIND);
            final String streamId = MapUtils.get(body, Constant.STREAM_ID);
            final String consumerId = MapUtils.get(body, Constant.CONSUMER_ID);
            final String consumeClientId = MapUtils.get(body, Constant.CLIENT_ID);
            final ClientWrapper consumeClientWrapper = room.clientWrapper(consumeClientId);
            final Map<String, Consumer> consumers = consumeClientWrapper.getConsumers();    
            final Map<String, Consumer> producerConsumers = producer.getConsumers();
            final Consumer consumer = new Consumer(consumeClientWrapper, producer, kind, streamId, consumerId);
            final Consumer oldConsumer = consumers.put(producerId, consumer);
            final Consumer oldProducerConsumer = producerConsumers.put(consumerId, consumer);
            if(oldConsumer != null || oldProducerConsumer != null) {
                log.warn("消费者已经存在：{}", consumerId);
                // TODO：关闭旧的？
            }
            final Client consumeClient = consumeClientWrapper.getClient();
            consumeClient.push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
    /**
     * 消费媒体
     * 
     * @param room 房间
     * @param consumeClientWrapper 消费者终端包装器
     * @param producer 生产者
     */
    private void consume(Room room, ClientWrapper consumeClientWrapper, Producer producer) {
        if(consumeClientWrapper.consume(producer)) {
            // TODO：回调媒体服务准备完成
            if(log.isDebugEnabled()) {
                log.debug("已经消费媒体：{} - {}", consumeClientWrapper.getClientId(), producer.getStreamId());
            }
            return;
        } else {
            if(log.isDebugEnabled()) {
                log.debug("消费媒体：{} - {}", consumeClientWrapper.getClientId(), producer.getStreamId());
            }
        }
        final String clientId = consumeClientWrapper.getClientId();
        final String streamId = producer.getStreamId() + "->" + clientId;
        final Client mediaClient = room.getMediaClient();
        final Transport recvTransport = consumeClientWrapper.getRecvTransport();
        final ClientWrapper produceClientWrapper = producer.getProduceClient();
        final Map<String, Object> body = new HashMap<>();
        body.put(Constant.ROOM_ID, room.getRoomId());
        body.put(Constant.CLIENT_ID, clientId);
        body.put(Constant.SOURCE_ID, produceClientWrapper.getClientId());
        body.put(Constant.STREAM_ID, streamId);
        body.put(Constant.PRODUCER_ID, producer.getProducerId());
        body.put(Constant.TRANSPORT_ID, recvTransport.getTransportId());
        body.put(Constant.RTP_CAPABILITIES, consumeClientWrapper.getRtpCapabilities());
        body.put(Constant.SCTP_CAPABILITIES, consumeClientWrapper.getSctpCapabilities());
        mediaClient.push(this.build(body));
    }

}
