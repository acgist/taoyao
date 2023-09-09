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
import com.acgist.taoyao.signal.party.media.Transport;
import com.acgist.taoyao.signal.party.media.Transport.Direction;
import com.acgist.taoyao.signal.party.room.ClientWrapper;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 创建RTP输入通道信令
 * 
 * TODO：优化接收通道
 * 
 * 注意：
 * 1. ffmpeg不支持rtcpMux
 * 2. comedia必须开启srtp功能
 * 3. 如果关闭comedia不会自动升级双向通道
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    memo = "用来接入RTP协议终端",
    body = """
    {
        "roomId"         : "房间ID",
        "rtcpMux"        : RTP/RTCP端口复用（true|false）,
        "comedia"        : 自动识别终端端口（true|false）,
        "enableSctp"     : 是否开启SCTP（true|false）,
        "numSctpStreams" : SCTP数量,
        "enableSrtp"     : 是否开启SRTP（true|false）,
        "srtpCryptoSuite": {
            "cryptoSuite": "算法（AEAD_AES_256_GCM|AEAD_AES_128_GCM|AES_CM_128_HMAC_SHA1_80|AES_CM_128_HMAC_SHA1_32）",
            "keyBase64"  : "密钥"
        }
    }
    {
        roomId     : "房间ID",
        transportId: "通道ID",
        ip         : "RTP监听IP",
        port       : "RTP媒体端口",
        rtcpPort   : "RTP媒体RTCP端口"
    }
    """,
    flow = "终端=>信令服务->媒体服务"
)
public class MediaTransportPlainCreateProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::transport::plain::create";
    
    public MediaTransportPlainCreateProtocol() {
        super("创建RTP输入通道信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        body.put(Constant.CLIENT_ID, clientId);
        final Message response = room.requestMedia(message);
        final Map<String, Object> responseBody  = response.body();
        final Map<String, Transport> transports = room.getTransports();
        final String transportId = MapUtils.get(responseBody, Constant.TRANSPORT_ID);
        // 重写地址
        this.rewriteIp(client.getIP(), responseBody);
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
        // 消费者
        Transport recvTransport = clientWrapper.getRecvTransport();
        if(recvTransport == null) {
            recvTransport = new Transport(transportId, Direction.RECV, room, client);
            // transports.put(transportId, recvTransport);
            // 消费媒体
            // this.publishEvent(new MediaConsumeEvent(room, clientWrapper));
        } else {
            log.warn("接收通道已经存在：{}", transportId);
        }
        clientWrapper.setRecvTransport(recvTransport);
        client.push(response);
        log.info("{}创建RTP输入通道：{}", clientId, transportId);
    }
    
    /**
     * 重写IP地址
     * 
     * @param clientIp 终端IP
     * @param body     消息主体
     */
    private void rewriteIp(String clientIp, Map<String, Object> body) {
        // 媒体服务返回IP
        final String mediaIp = (String) body.get(Constant.IP);
        if(StringUtils.isNotEmpty(mediaIp)) {
            final String rewriteIp = NetUtils.rewriteIP(mediaIp, clientIp);
            log.debug("重写地址：{} + {} -> {}", mediaIp, clientIp, rewriteIp);
            body.put(Constant.IP, rewriteIp);
        }
    }

}
