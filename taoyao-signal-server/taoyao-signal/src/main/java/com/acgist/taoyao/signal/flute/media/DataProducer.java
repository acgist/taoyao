package com.acgist.taoyao.signal.flute.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据生产者
 * 
 * @author acgist
 */
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
    
}
