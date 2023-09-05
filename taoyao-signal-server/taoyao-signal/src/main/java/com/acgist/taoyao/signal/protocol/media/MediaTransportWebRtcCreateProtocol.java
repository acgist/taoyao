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
import com.acgist.taoyao.signal.event.media.MediaConsumeEvent;
import com.acgist.taoyao.signal.party.media.Transport;
import com.acgist.taoyao.signal.party.media.Transport.Direction;
import com.acgist.taoyao.signal.party.room.ClientWrapper;
import com.acgist.taoyao.signal.party.room.Room;
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
            "roomId"          : "房间标识",
            "forceTcp"        : "强制使用TCP",
            "producing"       : "是否生产",
            "consuming"       : "是否消费",
            "sctpCapabilities": "sctpCapabilities"
        }
        {
            "roomId"        : "房间标识",
            "transportId"   : "传输通道标识",
            "iceCandidates" : "iceCandidates",
            "iceParameters" : "iceParameters",
            "dtlsParameters": "dtlsParameters",
            "sctpParameters": "sctpParameters"
        }
        """
    },
    flow = "终端=>信令服务->媒体服务"
)
public class MediaTransportWebRtcCreateProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::webrtc::create";
    
    public MediaTransportWebRtcCreateProtocol() {
        super("创建WebRTC通道信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            body.put(Constant.CLIENT_ID, clientId);
            final Message response = room.requestMedia(message);
            final Map<String, Object> responseBody  = response.body();
            final Map<String, Transport> transports = room.getTransports();
            final String transportId = MapUtils.get(responseBody, Constant.TRANSPORT_ID);
            // 重写地址
            this.rewriteIP(client.getIP(), responseBody);
            // 处理逻辑
            final ClientWrapper clientWrapper = room.clientWrapper(client);
            // 消费者
            final Boolean consuming = MapUtils.getBoolean(body, Constant.CONSUMING);
            if(Boolean.TRUE.equals(consuming)) {
                Transport recvTransport = clientWrapper.getRecvTransport();
                if(recvTransport == null) {
                    recvTransport = new Transport(transportId, Direction.RECV, room, client);
                    transports.put(transportId, recvTransport);
                } else {
                    log.warn("接收通道已经存在：{}", transportId);
                }
                clientWrapper.setRecvTransport(recvTransport);
                // 拷贝属性
                recvTransport.copy(responseBody);
                // 消费媒体：不能在连接时调用
                this.publishEvent(new MediaConsumeEvent(room, clientWrapper));
            }
            // 生产者
            final Boolean producing = MapUtils.getBoolean(body, Constant.PRODUCING);
            if(Boolean.TRUE.equals(producing)) {
                Transport sendTransport = clientWrapper.getSendTransport();
                if(sendTransport == null) {
                    sendTransport = new Transport(transportId, Direction.SEND, room, client);
                    transports.put(transportId, sendTransport);
                } else {
                    log.warn("发送通道已经存在：{}", transportId);
                }
                clientWrapper.setSendTransport(sendTransport);
                // 拷贝属性
                sendTransport.copy(responseBody);
            }
            client.push(response);
            log.info("{}创建WebRTC通道信令：{}", clientId, transportId);
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
    /**
     * 重写IP地址
     * 
     * @param clientIP 终端IP
     * @param body     消息主体
     */
    private void rewriteIP(String clientIP, Map<String, Object> body) {
        final List<Map<Object, Object>> iceCandidates = MapUtils.get(body, Constant.ICE_CANDIDATES);
        if(CollectionUtils.isEmpty(iceCandidates)) {
            return;
        }
        iceCandidates.forEach(map -> {
            // 媒体服务返回IP
            final String mediaIP = (String) map.get(Constant.IP);
            if(StringUtils.isNotEmpty(mediaIP)) {
                final String rewriteIP = NetUtils.rewriteIP(mediaIP, clientIP);
                log.debug("重写地址：{} + {} -> {}", mediaIP, clientIP, rewriteIP);
                map.put(Constant.IP, rewriteIP);
            }
        });
    }
    
}
