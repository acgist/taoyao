package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.DataProducer;
import com.acgist.taoyao.signal.party.room.ClientWrapper;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 生产数据信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = {
        """
        {
            "roomId"     : "房间标识",
            "transportId": "通道标识"
        }
        {
            "roomId"    : "房间ID",
            "producerId": "生产者ID",
        }
        """
    },
    flow = "终端=>信令服务->媒体服务"
)
public class MediaDataProduceProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::data::produce";
    
    public MediaDataProduceProtocol() {
        super("生产数据信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            final String streamId = Constant.STREAM_ID_PRODUCER.apply(Constant.DATA, clientId);
            body.put(Constant.CLIENT_ID, clientId);
            body.put(Constant.STREAM_ID, streamId);
            final Message response = room.requestMedia(message);
            final Map<String, Object> responseBody = response.body();
            final String producerId = MapUtils.get(responseBody, Constant.PRODUCER_ID);
            final ClientWrapper producerClientWrapper           = room.clientWrapper(client);
            final Map<String, DataProducer> roomDataProducers   = room.getDataProducers();
            final Map<String, DataProducer> clientDataProducers = producerClientWrapper.getDataProducers();
            final DataProducer dataProducer          = new DataProducer(streamId, producerId, room, producerClientWrapper);
            final DataProducer oldRoomDataProducer   = roomDataProducers.put(producerId, dataProducer);
            final DataProducer oldClientDataProducer = clientDataProducers.put(producerId, dataProducer);
            if(oldRoomDataProducer != null || oldClientDataProducer != null) {
                log.warn("数据生产者已经存在：{}", producerId);
            }
            final Message responseMessage = response.cloneWithoutBody();
            responseMessage.setBody(Map.of(
                Constant.STREAM_ID,   streamId,
                Constant.PRODUCER_ID, producerId
            ));
            client.push(responseMessage);
            log.info("{}生产数据：{} - {}", clientId, streamId, producerId);
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
