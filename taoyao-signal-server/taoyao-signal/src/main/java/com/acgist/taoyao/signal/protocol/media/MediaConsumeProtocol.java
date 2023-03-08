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
import com.acgist.taoyao.signal.event.media.MediaConsumeEvent;
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
    消费媒体：主动消费、终端生产媒体、终端创建WebRTC消费通道
    终端生产媒体当前房间所有终端根据订阅类型自动消费媒体
    终端创建WebRTC消费通道根据订阅类型自动消费当前房间已有媒体
    """,
    flow = {
        "终端-[生产媒体]>信令服务-[其他终端消费])信令服务",
        "终端-[创建WebRTC消费通道]>信令服务-[消费其他终端])信令服务",
        "终端->信令服务->媒体服务=>信令服务->终端->信令服务->媒体服务"
    }
)
public class MediaConsumeProtocol extends ProtocolRoomAdapter implements ApplicationListener<MediaConsumeEvent> {

    public static final String SIGNAL = "media::consume";
    
    public MediaConsumeProtocol() {
        super("消费媒体信令", SIGNAL);
    }
    
    @Async
    @Override
    public void onApplicationEvent(MediaConsumeEvent event) {
        final Room room = event.getRoom();
        if(event.getProducer() != null) {
            // 生产媒体：其他终端消费
            final Producer producer = event.getProducer();
            final ClientWrapper produceClientWrapper = producer.getProducerClient();
            room.getClients().values().stream()
            .filter(v -> v != produceClientWrapper)
            .filter(v -> v.getSubscribeType().canConsume(producer))
            .forEach(v -> this.consume(room, v, producer, this.build()));
        } else if(event.getClientWrapper() != null) {
            // 创建WebRTC消费通道：消费其他终端
            final ClientWrapper consumeClientWrapper = event.getClientWrapper();
            room.getClients().values().stream()
            .filter(v -> v != consumeClientWrapper)
            .flatMap(v -> v.getProducers().values().stream())
            .filter(v -> consumeClientWrapper.getSubscribeType().canConsume(v))
            .forEach(producer -> this.consume(room, consumeClientWrapper, producer, this.build()));
        } else {
            throw MessageCodeException.of("消费媒体失败");
        }
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String producerId = MapUtils.get(body, Constant.PRODUCER_ID);
        final Producer producer = room.producer(producerId);
        if(clientType.mediaClient()) {
            // 主动请求消费 || 消费通道准备就绪
            this.consume(room, room.clientWrapper(client), producer, message);
        } else if(clientType.mediaServer()) {
            // 媒体通道准备就绪
            final String kind = MapUtils.get(body, Constant.KIND);
            final String streamId = MapUtils.get(body, Constant.STREAM_ID);
            final String consumerId = MapUtils.get(body, Constant.CONSUMER_ID);
            final String consumerClientId = MapUtils.get(body, Constant.CLIENT_ID);
            final ClientWrapper consumerClientWrapper = room.clientWrapper(consumerClientId);
            final Map<String, Consumer> consumers = consumerClientWrapper.getConsumers();    
            final Map<String, Consumer> producerConsumers = producer.getConsumers();
            final Consumer consumer = new Consumer(kind, streamId, consumerId, room, producer, consumerClientWrapper);
            final Consumer oldConsumer = consumers.put(producerId, consumer);
            final Consumer oldProducerConsumer = producerConsumers.put(consumerId, consumer);
            if(oldConsumer != null || oldProducerConsumer != null) {
                log.warn("消费者已经存在：{}", consumerId);
            }
            final Client consumeClient = consumerClientWrapper.getClient();
            consumeClient.push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
    /**
     * 消费媒体
     * 
     * @param room 房间
     * @param consumerClientWrapper 消费者终端包装器
     * @param producer 生产者
     * @param message 消息
     */
    private void consume(Room room, ClientWrapper consumerClientWrapper, Producer producer, Message message) {
        final Client mediaClient = room.getMediaClient();
        if(consumerClientWrapper.consumed(producer)) {
            // TODO：没有清理干净
            // 消费通道准备就绪
            if(log.isDebugEnabled()) {
                log.debug("消费通道准备就绪：{} - {}", consumerClientWrapper.getClientId(), producer.getStreamId());
            }
            mediaClient.push(message);
        } else {
            // 主动消费媒体
            if(log.isDebugEnabled()) {
                log.debug("消费媒体：{} - {}", consumerClientWrapper.getClientId(), producer.getStreamId());
            }
            final String clientId = consumerClientWrapper.getClientId();
            final String streamId = producer.getStreamId() + "->" + clientId;
            final Transport recvTransport = consumerClientWrapper.getRecvTransport();
            final ClientWrapper produceClientWrapper = producer.getProducerClient();
            final Map<String, Object> body = new HashMap<>();
            body.put(Constant.ROOM_ID, room.getRoomId());
            body.put(Constant.CLIENT_ID, clientId);
            body.put(Constant.SOURCE_ID, produceClientWrapper.getClientId());
            body.put(Constant.STREAM_ID, streamId);
            body.put(Constant.PRODUCER_ID, producer.getProducerId());
            body.put(Constant.TRANSPORT_ID, recvTransport.getTransportId());
            body.put(Constant.RTP_CAPABILITIES, consumerClientWrapper.getRtpCapabilities());
            body.put(Constant.SCTP_CAPABILITIES, consumerClientWrapper.getSctpCapabilities());
            message.setBody(body);
            mediaClient.push(message);
        }
    }

}
