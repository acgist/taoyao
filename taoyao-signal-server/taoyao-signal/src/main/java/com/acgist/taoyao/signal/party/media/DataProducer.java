package com.acgist.taoyao.signal.party.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据生产者
 * 
 * @author acgist
 */
@Slf4j
@Setter
@Getter
public class DataProducer {

    /**
     * 生产者终端
     */
    private final ClientWrapper produceClient;
    /**
     * 数据流ID
     */
    private final String streamId;
    /**
     * 生产者标识
     */
    private final String producerId;
    /**
     * 消费者
     */
    private final Map<String, DataConsumer> dataConsumers;
    
    public DataProducer(ClientWrapper produceClient, String streamId, String producerId) {
        this.produceClient = produceClient;
        this.streamId = streamId;
        this.producerId = producerId;
        this.dataConsumers = new ConcurrentHashMap<>();
    }
    
    /**
     * 记录日志
     */
    public void log() {
        log.debug("当前数据生产者：{} - {}", this.producerId, this.streamId);
        this.dataConsumers.values().forEach(DataConsumer::log);
    }
    
}
