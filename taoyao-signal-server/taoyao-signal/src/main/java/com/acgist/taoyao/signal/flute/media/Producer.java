package com.acgist.taoyao.signal.flute.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

/**
 * 生产者
 * 
 * @author acgist
 */
@Getter
@Setter
public class Producer {

    /**
     * 生产者终端
     */
    private final ClientWrapper client;
    /**
     * 媒体类型
     */
    private final Kind kind;
    /**
     * 媒体流ID
     */
    private final String streamId;
    /**
     * 生产者标识
     */
    private final String producerId;
    /**
     * 消费者
     */
    private final Map<String, Consumer> consumers;
    
    public Producer(ClientWrapper client, String kind, String streamId, String producerId) {
        this.client = client;
        this.kind = Kind.of(kind);
        this.streamId = streamId;
        this.producerId = producerId;
        this.consumers = new ConcurrentHashMap<>();
    }

}
