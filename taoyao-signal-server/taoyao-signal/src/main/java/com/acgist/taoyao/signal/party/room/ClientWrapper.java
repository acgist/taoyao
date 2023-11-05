package com.acgist.taoyao.signal.party.room;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.party.media.Consumer;
import com.acgist.taoyao.signal.party.media.DataConsumer;
import com.acgist.taoyao.signal.party.media.DataProducer;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.media.Transport;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 终端包装器
 * 进入房间后封装相关对象
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class ClientWrapper implements AutoCloseable {

    /**
     * 房间
     */
    private final Room room;
    /**
     * 终端
     */
    private final Client client;
    /**
     * 房间ID
     */
    private final String roomId;
    /**
     * 终端ID
     */
    private final String clientId;
    /**
     * 媒体订阅类型
     */
    private SubscribeType subscribeType;
    /**
     * RTP协商
     */
    private Object rtpCapabilities;
    /**
     * SCTP协商
     */
    private Object sctpCapabilities;
    /**
     * 服务端媒体录像机
     */
    private Recorder recorder;
    /**
     * 发送通道
     */
    private Transport sendTransport;
    /**
     * 接收通道
     */
    private Transport recvTransport;
    /**
     * 生产者
     * 其他终端消费当前终端的生产者
     */
    private final Map<String, Producer> producers;
    /**
     * 消费者
     * 当前终端消费其他终端的消费者
     */
    private final Map<String, Consumer> consumers;
    /**
     * 数据生产者
     * 其他终端消费当前终端的生产者
     */
    private final Map<String, DataProducer> dataProducers;
    /**
     * 数据消费者
     * 当前终端消费其他终端的消费者
     */
    private final Map<String, DataConsumer> dataConsumers;
    
    public ClientWrapper(Room room, Client client) {
        this.room          = room;
        this.client        = client;
        this.roomId        = room.getRoomId();
        this.clientId      = client.getClientId();
        this.producers     = new ConcurrentHashMap<>();
        this.consumers     = new ConcurrentHashMap<>();
        this.dataProducers = new ConcurrentHashMap<>();
        this.dataConsumers = new ConcurrentHashMap<>();
    }
    
    /**
     * @param producer 生产者
     * 
     * @return 是否已经消费生产者
     */
    public boolean consumed(Producer producer) {
        return this.consumers.values().stream()
            .anyMatch(v -> v.getProducer() == producer);
    }
    
    /**
     * @param dataProducer 数据生产者
     * 
     * @return 是否已经消费数据生产者
     */
    public boolean consumedData(DataProducer dataProducer) {
        return this.dataConsumers.values().stream()
            .anyMatch(v -> v.getDataProducer() == dataProducer);
    }
    
    /**
     * 推送消息
     * 
     * @param message 消息
     */
    public void push(Message message) {
        this.client.push(message);
    }
    
    /**
     * 请求消息
     * 
     * @param message 请求
     * 
     * @return 响应
     */
    public Message request(Message message) {
        return this.client.request(message);
    }
    
    @Override
    public void close() {
        // 注意：不要关闭终端（只是离开房间）
        if(this.recorder != null) {
            this.recorder.close();
        }
        this.consumers.values().forEach(Consumer::close);
        this.producers.values().forEach(Producer::close);
        this.dataConsumers.values().forEach(DataConsumer::close);
        this.dataProducers.values().forEach(DataProducer::close);
        if(this.recvTransport != null) {
            this.recvTransport.close();
        }
        if(this.sendTransport != null) {
            this.sendTransport.close();
        }
    }

    /**
     * 记录日志
     */
    public void log() {
        log.info("""
            当前终端：{}
            消费者数量：{}
            生产者数量：{}
            数据消费者数量：{}
            数据生产者数量：{}""",
            this.clientId,
            this.consumers.size(),
            this.producers.size(),
            this.dataConsumers.size(),
            this.dataProducers.size()
        );
        this.consumers.values().forEach(Consumer::log);
        this.producers.values().forEach(Producer::log);
        this.dataConsumers.values().forEach(DataConsumer::log);
        this.dataProducers.values().forEach(DataProducer::log);
    }
    
}
