package com.acgist.taoyao.signal.party.media;

import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.media.TransportCloseEvent;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 传输通道
 * 注意：正常情况不会调用
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Transport extends OperatorAdapter {

    /**
     * 方向
     * 
     * @author acgist
     */
    public enum Direction {
        
        // 接收
        RECV,
        // 发送
        SEND;
        
    }
    
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
     * 方向
     */
    private final Direction direction;
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
    
    public Transport(String transportId, Direction direction, Room room, Client client) {
        this.transportId = transportId;
        this.direction = direction;
        this.room = room;
        this.client = client;
        this.roomId = room.getRoomId();
        this.clientId = client.getClientId();
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
        log.info("关闭传输通道：{} - {}", this.transportId, this.direction);
        EventPublisher.publishEvent(new TransportCloseEvent(this.transportId, this.room));
    }
    
    @Override
    public void remove() {
        log.info("移除传输通道：{} - {}", this.transportId, this.direction);
        this.room.getTransports().remove(this.transportId);
    }

}
