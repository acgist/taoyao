package com.acgist.taoyao.signal.party.media;

import lombok.Getter;
import lombok.Setter;

/**
 * 消费者
 * 
 * @author acgist
 */
@Getter
@Setter
public class Consumer {

    /**
     * 消费者终端
     */
    private final ClientWrapper consumeClient;
    /**
     * 生产者
     */
    private final Producer producer;
    /**
     * 媒体类型
     */
    private final Kind kind;
    /**
     * 媒体流ID
     */
    private final String streamId;
    /**
     * 消费者标识
     */
    private final String consumerId;
    
    public Consumer(ClientWrapper consumeClient, Producer producer, String kind, String streamId, String consumerId) {
        this.consumeClient = consumeClient;
        this.producer = producer;
        this.kind = Kind.of(kind);
        this.streamId = streamId;
        this.consumerId = consumerId;
    }
    
}
