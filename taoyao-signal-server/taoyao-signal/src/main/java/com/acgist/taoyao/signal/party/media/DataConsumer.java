package com.acgist.taoyao.signal.party.media;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据消费者
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class DataConsumer {

    /**
     * 消费者终端
     */
    private final ClientWrapper consumeClient;
    /**
     * 生产者
     */
    private final Producer producer;
    /**
     * 数据流ID
     */
    private final String streamId;
    /**
     * 消费者标识
     */
    private final String consumerId;
    
    public DataConsumer(ClientWrapper consumeClient, Producer producer, String streamId, String consumerId) {
        this.consumeClient = consumeClient;
        this.producer = producer;
        this.streamId = streamId;
        this.consumerId = consumerId;
    }
    
    /**
     * 记录日志
     */
    public void log() {
        log.debug("当前数据消费者：{} - {}", this.consumerId, this.streamId);
    }
    
}
