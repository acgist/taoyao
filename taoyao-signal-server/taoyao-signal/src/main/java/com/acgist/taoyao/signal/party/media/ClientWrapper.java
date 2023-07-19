package com.acgist.taoyao.signal.party.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 终端包装器：Peer
 * 视频房间使用
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
	 * 房间标识
	 */
	private final String roomId;
	/**
	 * 终端标识
	 */
	private final String clientId;
	/**
	 * 媒体订阅类型
	 * 指定订阅类型终端注册或者生成媒体后会自动进行媒体推流拉流
	 * 没有订阅任何媒体时需要用户自己对媒体进行消费控制
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
	 * 媒体录像
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
	 * 其他终端消费当前终端的消费者
	 */
	private final Map<String, Producer> producers;
    /**
     * 消费者
     * 当前终端消费其他终端的消费者
     */
    private final Map<String, Consumer> consumers;
	/**
	 * 数据生产者
	 * 其他终端消费当前终端的消费者
	 */
	private final Map<String, DataProducer> dataProducers;
	/**
	 * 数据消费者
	 * 当前终端消费其他终端的消费者
	 */
	private final Map<String, DataConsumer> dataConsumers;
	
    public ClientWrapper(Room room, Client client) {
        this.room = room;
        this.client = client;
        this.roomId = room.getRoomId();
        this.clientId = client.getClientId();
        this.producers = new ConcurrentHashMap<>();
        this.consumers = new ConcurrentHashMap<>();
        this.dataProducers = new ConcurrentHashMap<>();
        this.dataConsumers = new ConcurrentHashMap<>();
    }
	
    /**
     * @return 生产者数量
     */
    public Integer producerSize() {
        return this.producers.size();
    }
    
    /**
     * @return 消费者数量
     */
    public Integer consumerSize() {
        return this.producers.values().stream()
            .map(producer -> producer.getConsumers().size())
            .collect(Collectors.counting())
            .intValue();
    }
    
    /**
     * @param producer 生产者
     * 
     * @return 是否已经消费
     */
    public boolean consumed(Producer producer) {
        return this.consumers.values().stream()
            .anyMatch(v -> v.getProducer() == producer);
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
