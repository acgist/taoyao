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
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;
import com.acgist.taoyao.signal.terminal.media.ClientWrapper;
import com.acgist.taoyao.signal.terminal.media.Producer;
import com.acgist.taoyao.signal.terminal.media.Room;
import com.acgist.taoyao.signal.terminal.media.Transport;

/**
 * 消费媒体信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    flow = {
        "信令服务->媒体服务=>信令服务=>终端->信令服务->信令服务",
        "终端->信令服务->媒体服务=>信令服务=>终端->信令服务->信令服务"
    }
)
public class MediaConsumeProtocol extends ProtocolRoomAdapter implements ApplicationListener<MediaProduceEvent> {

    public static final String SIGNAL = "media::consume";
    
    protected MediaConsumeProtocol() {
        super("消费媒体信令", SIGNAL);
    }
    
    @Async
    @Override
    public void onApplicationEvent(MediaProduceEvent event) {
        // TODO：根据类型进行消费
        final Room room = event.getRoom();
        final Client client = event.getClient();
        final Producer producer = event.getProducer();
        room.getClients().keySet().stream()
        .filter(v -> v != client)
        .forEach(v -> this.consume(room, v, producer));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String kind = MapUtils.get(body, Constant.KIND);
        final String producerId = MapUtils.get(body, Constant.PRODUCER_ID);
        final Producer producer = room.producer(producerId);
        final String streamId = producer.getStreamId() + "->" + clientId;
        if(clientType == ClientType.WEB || clientType == ClientType.CAMERA) {
            // 请求消费
        } else if(clientType == ClientType.MEDIA) {
            // 等待消费者准备完成
            
        } else {
        }
    }
    
    private void consume(Room room, Client client, Producer producer) {
        final Client mediaClient = room.getMediaClient();
        final ClientWrapper clientWrapper = room.client(client);
        final Transport recvTransport = clientWrapper.getRecvTransport();
        final Map<String, Object> body = new HashMap<>();
        body.put(Constant.ROOM_ID, room.getRoomId());
        body.put(Constant.PRODUCER_ID, producer.getProducerId());
        body.put(Constant.TRANSPORT_ID, recvTransport.getTransportId());
        body.put(Constant.RTP_CAPABILITIES, clientWrapper.getRtpCapabilities());
        body.put(Constant.SCTP_CAPABILITIES, clientWrapper.getSctpCapabilities());
        mediaClient.push(this.build(body));
    }

}
