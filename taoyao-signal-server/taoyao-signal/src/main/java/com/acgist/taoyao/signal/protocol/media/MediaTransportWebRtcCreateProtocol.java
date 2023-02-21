package com.acgist.taoyao.signal.protocol.media;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.config.MediaServerProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.service.IpService;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Peer;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.media.Transport;
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
    
    @Autowired
    private IpService ipService;
    
    protected MediaTransportWebRtcCreateProtocol() {
        super("创建WebRTC通道信令", SIGNAL);
    }

    @Override
    public void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message) {
    }

    @Override
    public void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message) {
        final Message response = room.request(message);
        final Peer peer = client.peer();
        final Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        final MediaServerProperties mediaServerProperties = room.getMediaClient().mediaServerProperties();
        if(Boolean.TRUE.equals(mediaServerProperties.getRewriteIp())) {
            // 重写IP地址
            this.rewriteIp(client.ip(), responseBody, mediaServerProperties);
        }
        final Map<String, Transport> transports = peer.getTransports();
        final String transportId = this.get(responseBody, Constant.TRANSPORT_ID);
        final Transport transport = transports.computeIfAbsent(transportId, key -> new Transport(client));
        // 拷贝属性
        transport.copy(responseBody);
        client.push(response);
    }
    
    /**
     * 重写IP地址
     * 
     * @param clientIp 终端IP
     * @param body 消息主体
     * @param mediaServerProperties 媒体服务配置
     */
    private void rewriteIp(String clientIp, Map<?, ?> body, MediaServerProperties mediaServerProperties) {
        final List<Map<Object, Object>> iceCandidates = this.get(body, Constant.ICE_CANDIDATES);
        if(CollectionUtils.isEmpty(iceCandidates)) {
            return;
        }
        final String defaultMediaIp = mediaServerProperties.getHost();
        iceCandidates.forEach(map -> {
            final String mediaIp = (String) map.get(Constant.IP);
            if(StringUtils.isNotEmpty(mediaIp)) {
                final String rewriteIp = this.ipService.rewriteIp(mediaIp, clientIp, defaultMediaIp);
                log.debug("重写IP地址：{} | {} + {} -> {}", mediaIp, defaultMediaIp, clientIp, rewriteIp);
                map.put(Constant.IP, rewriteIp);
            }
        });
    }

}
