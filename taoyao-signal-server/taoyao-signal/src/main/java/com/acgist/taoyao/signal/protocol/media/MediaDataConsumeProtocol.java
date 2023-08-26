package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.DataConsumer;
import com.acgist.taoyao.signal.party.media.DataProducer;
import com.acgist.taoyao.signal.party.media.Transport;
import com.acgist.taoyao.signal.party.room.ClientWrapper;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 消费数据信令
 * 
 * TODO：防止重复消费
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    memo = """
    数据通道消费者不会自动创建，需要用户自己订阅生产者。
    """,
    body = """
    {
        "roomId"    : "房间ID"
        "producerId": "生产者ID",
    }
    """,
    flow = {
        "终端->信令服务->媒体服务->信令服务->终端"
    }
)
public class MediaDataConsumeProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::data::consume";
    
    public MediaDataConsumeProtocol() {
        super("消费数据信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String producerId = MapUtils.get(body, Constant.PRODUCER_ID);
        final DataProducer dataProducer = room.dataProducer(producerId);
        if(dataProducer == null) {
            throw MessageCodeException.of("没有提供数据生产：" + producerId);
        }
        if(clientType.isClient()) {
            final ClientWrapper dataConsumerClientWrapper = room.clientWrapper(client);
            final String dataConsumerClientId             = dataConsumerClientWrapper.getClientId();
            final ClientWrapper dataProducerClientWrapper = dataProducer.getProducerClient();
            final String dataProducerClientId             = dataProducerClientWrapper.getClientId();
            final Transport recvTransport                 = dataConsumerClientWrapper.getRecvTransport();
            final String streamId                         = Constant.STREAM_ID_CONSUMER.apply(dataProducer.getStreamId(), dataConsumerClientId);
            body.put(Constant.ROOM_ID,           room.getRoomId());
            body.put(Constant.CLIENT_ID,         dataConsumerClientId);
            body.put(Constant.SOURCE_ID,         dataProducerClientId);
            body.put(Constant.STREAM_ID,         streamId);
            body.put(Constant.PRODUCER_ID,       dataProducer.getProducerId());
            body.put(Constant.TRANSPORT_ID,      recvTransport.getTransportId());
            body.put(Constant.RTP_CAPABILITIES,  dataConsumerClientWrapper.getRtpCapabilities());
            body.put(Constant.SCTP_CAPABILITIES, dataConsumerClientWrapper.getSctpCapabilities());
            mediaClient.push(message);
        } else if(clientType.isMedia()) {
            final String streamId   = MapUtils.get(body, Constant.STREAM_ID);
            final String consumerId = MapUtils.get(body, Constant.CONSUMER_ID);
            final String dataConsumerClientId             = MapUtils.get(body, Constant.CLIENT_ID);
            final ClientWrapper dataConsumerClientWrapper = room.clientWrapper(dataConsumerClientId);
            final Map<String, DataConsumer> roomDataConsumers     = room.getDataConsumers();
            final Map<String, DataConsumer> clientDataConsumers   = dataConsumerClientWrapper.getDataConsumers();   
            final Map<String, DataConsumer> producerDataConsumers = dataProducer.getDataConsumers();
            final DataConsumer dataConsumer            = new DataConsumer(streamId, consumerId, room, dataProducer, dataConsumerClientWrapper);
            final DataConsumer oldDataRoomConsumer     = roomDataConsumers.put(consumerId, dataConsumer);
            final DataConsumer oldDataClientConsumer   = clientDataConsumers.put(consumerId, dataConsumer);
            final DataConsumer oldDataProducerConsumer = producerDataConsumers.put(consumerId, dataConsumer);
            if(oldDataRoomConsumer != null || oldDataClientConsumer != null || oldDataProducerConsumer != null) {
                log.warn("消费者已经存在：{}", consumerId);
            }
            final Client consumeClient = dataConsumerClientWrapper.getClient();
            consumeClient.push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
