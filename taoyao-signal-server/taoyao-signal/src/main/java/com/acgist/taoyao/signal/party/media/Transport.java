package com.acgist.taoyao.signal.party.media;

import java.io.Closeable;
import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * 传输通道
 * 
 * @author acgist
 */
@Getter
@Setter
public class Transport implements Closeable {

    /**
     * 房间
     */
    private final Room room;
    /**
     * 终端
     */
    private final Client client;
    /**
     * 房间标识
     */
    private final String roomId;
    /**
     * 终端标识
     */
    private final String clientId;
    /**
     * 通道标识
     */
    private final String transportId;
    /**
     * ICE协商
     */
    private Object iceCandidates;
    /**
     * ICE参数
     */
    private Object iceParameters;
    /**
     * DTLS参数
     */
    private Object dtlsParameters;
    /**
     * SCTP参数
     */
    private Object sctpParameters;
    
    public Transport(String transportId, Room room, Client client) {
        this.transportId = transportId;
        this.room = room;
        this.client = client;
        this.roomId = room.getRoomId();
        this.clientId = client.clientId();
    }
    
    /**
     * 拷贝属性
     * 
     * @param body 消息主体
     */
    public void copy(Map<String, Object> body) {
        this.iceCandidates = MapUtils.get(body, Constant.ICE_CANDIDATES);
        this.iceParameters = MapUtils.get(body, Constant.ICE_PARAMETERS);
        this.dtlsParameters = MapUtils.get(body, Constant.DTLS_PARAMETERS);
        this.sctpParameters = MapUtils.get(body, Constant.SCTP_PARAMETERS);
    }
    
    @Override
    public void close() {
        // TODO：实现
    }

}
