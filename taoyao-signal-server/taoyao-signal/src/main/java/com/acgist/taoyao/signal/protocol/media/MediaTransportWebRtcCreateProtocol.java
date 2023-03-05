package com.acgist.taoyao.signal.protocol.media;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.boot.utils.NetUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.MediaProduceEvent;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.party.media.Transport;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 创建WebRTC通道信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = {
        """
        {
            "roomId": "房间标识"
        }
        {
            "roomId": "房间标识",
            "transportId": "传输通道标识",
            "iceCandidates": "iceCandidates",
            "iceParameters": "iceParameters",
            "dtlsParameters": "dtlsParameters",
            "sctpParameters": "sctpParameters"
        }
        """
    },
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaTransportWebRtcCreateProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::webrtc::create";
    
    public MediaTransportWebRtcCreateProtocol() {
        super("创建WebRTC通道信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        body.put(Constant.CLIENT_ID, clientId);
        final Message response = room.request(message);
        final Map<String, Object> responseBody = response.body();
        final String transportId = MapUtils.get(responseBody, Constant.TRANSPORT_ID);
        // 重写地址
        this.rewriteIp(client.ip(), responseBody);
        // 处理逻辑
        final ClientWrapper clientWrapper = room.clientWrapper(client);
        // 消费者
        final Boolean consuming = MapUtils.getBoolean(body, Constant.CONSUMING);
        if(Boolean.TRUE.equals(consuming)) {
            Transport recvTransport = clientWrapper.getRecvTransport();
            if(recvTransport == null) {
                recvTransport = new Transport(transportId, room, client);
                clientWrapper.setRecvTransport(recvTransport);
            }
            // 拷贝属性
            recvTransport.copy(responseBody);
            this.publishEvent(new MediaProduceEvent(room, clientWrapper));
        }
        // 生产者
        final Boolean producing = MapUtils.getBoolean(body, Constant.PRODUCING);
        if(Boolean.TRUE.equals(producing)) {
            Transport sendTransport = clientWrapper.getSendTransport();
            if(sendTransport == null) {
                sendTransport = new Transport(transportId, room, client);
                clientWrapper.setSendTransport(sendTransport);
            }
            // 拷贝属性
            sendTransport.copy(responseBody);
        }
        client.push(response);
        log.info("{} 创建WebRTC信令通道：{}", clientId, transportId);
    }
    
    /**
     * 重写IP地址
     * 
     * @param clientIp 终端IP
     * @param body 消息主体
     */
    private void rewriteIp(String clientIp, Map<String, Object> body) {
        final List<Map<Object, Object>> iceCandidates = MapUtils.get(body, Constant.ICE_CANDIDATES);
        if(CollectionUtils.isEmpty(iceCandidates)) {
            return;
        }
        iceCandidates.forEach(map -> {
            // 媒体服务返回IP
            final String mediaIp = (String) map.get(Constant.IP);
            if(StringUtils.isNotEmpty(mediaIp)) {
                final String rewriteIp = NetUtils.rewriteIp(mediaIp, clientIp);
                log.debug("重写地址：{} + {} -> {}", mediaIp, clientIp, rewriteIp);
                map.put(Constant.IP, rewriteIp);
            }
        });
    }
    
}
