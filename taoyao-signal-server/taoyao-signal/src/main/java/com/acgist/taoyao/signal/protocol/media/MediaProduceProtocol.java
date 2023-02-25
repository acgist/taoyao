package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.MediaProduceEvent;
import com.acgist.taoyao.signal.flute.media.ClientWrapper;
import com.acgist.taoyao.signal.flute.media.Producer;
import com.acgist.taoyao.signal.flute.media.Room;
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
        // TODO；类型判断？
        final String kind = MapUtils.get(body, Constant.KIND);
        final String streamId = kind + "::" + clientId;
        body.put(Constant.CLIENT_ID, clientId);
        body.put(Constant.STREAM_ID, streamId);
        final Message response = room.request(message);
        final Map<String, Object> responseBody = response.mapBody();
        final String producerId = MapUtils.get(responseBody, Constant.PRODUCER_ID);
        final ClientWrapper clientWrapper = room.client(client);
        final Map<String, Producer> producers = clientWrapper.getProducers();
        final Producer producer = producers.computeIfAbsent(producerId, key -> new Producer(client, kind, streamId, producerId));
        final Message responseMessage = response.cloneWithoutBody();
        responseMessage.setBody(Map.of(
            Constant.KIND, kind,
            Constant.STREAM_ID, streamId,
            Constant.PRODUCER_ID, producerId
        ));
        // 根据不同类型进行推送：
        // 自动推送：不用广播
        // 音频全收：广播视频
        // 视频全收：广播音频
        // 全部不收：全部广播
        room.broadcast(responseMessage);
        log.info("{}生产媒体：{} - {} - {}", clientId, kind, streamId, producerId);
        this.publishEvent(new MediaProduceEvent(room, client, producer));
    }
    
}
