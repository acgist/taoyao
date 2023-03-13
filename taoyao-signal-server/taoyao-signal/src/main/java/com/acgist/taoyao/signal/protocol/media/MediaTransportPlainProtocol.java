package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.boot.utils.NetUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.party.media.Transport;
import com.acgist.taoyao.signal.party.media.Transport.Direction;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 创建RTP输入通道信令
 * 注意：
 * 3. ffmpeg不支持rtcpMux
 * 2. comedia必须开启srtp功能
 * 1. 如果关闭comedia不会自动升级双向通道
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID",
        "rtcpMux": RTP和RTCP端口复用（true|false）,
        "comedia": 自动终端端口（true|false）,
        "enableSctp": 是否开启sctp（true|false）,
        "numSctpStreams": sctp数量,
        "enableSrtp": 是否开启srtp（true|false）,
        "srtpCryptoSuite": {
            "cryptoSuite": "算法（AEAD_AES_256_GCM|AEAD_AES_128_GCM|AES_CM_128_HMAC_SHA1_80|AES_CM_128_HMAC_SHA1_32）",
            "keyBase64": "密钥"
        }
    }
    """
)
public class MediaTransportPlainProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::plain";
    
    public MediaTransportPlainProtocol() {
        super("创建RTP输入通道信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        body.put(Constant.CLIENT_ID, clientId);
        final Message response = room.request(message);
        final Map<String, Object> responseBody = response.body();
        final Map<String, Transport> transports = room.getTransports();
        final String transportId = MapUtils.get(responseBody, Constant.TRANSPORT_ID);
        // 重写地址
        this.rewriteIp(client.ip(), responseBody);
        // 处理逻辑
        final ClientWrapper clientWrapper = room.clientWrapper(client);
        // 生产者
        Transport sendTransport = clientWrapper.getSendTransport();
        if(sendTransport == null) {
            sendTransport = new Transport(transportId, Direction.SEND, room, client);
            transports.put(transportId, sendTransport);
        } else {
            log.warn("发送通道已经存在：{}", transportId);
        }
        clientWrapper.setSendTransport(sendTransport);
        // TODO：双向队列
        // 拷贝属性
        sendTransport.copy(responseBody);
        client.push(response);
        log.info("{}创建RTP信令通道：{}", clientId, transportId);
    }
    
    /**
     * 重写IP地址
     * 
     * @param clientIp 终端IP
     * @param body 消息主体
     */
    private void rewriteIp(String clientIp, Map<String, Object> body) {
        // 媒体服务返回IP
        final String mediaIp = (String) body.get(Constant.IP);
        if(StringUtils.isNotEmpty(mediaIp)) {
            final String rewriteIp = NetUtils.rewriteIp(mediaIp, clientIp);
            log.debug("重写地址：{} + {} -> {}", mediaIp, clientIp, rewriteIp);
            body.put(Constant.IP, rewriteIp);
        }
    }

}
