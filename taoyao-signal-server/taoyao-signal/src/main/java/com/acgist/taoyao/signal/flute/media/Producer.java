package com.acgist.taoyao.signal.flute.media;

import java.util.Map;

import com.acgist.taoyao.signal.client.Client;

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
     * 终端
     */
    private final Client client;
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
    private final String producerId;
    /**
     * 消费者
     */
    private Map<String, Consumer> consumers;
    
    public Producer(Client client, String kind, String streamId, String producerId) {
        this.client = client;
        this.kind = Kind.of(kind);
        this.streamId = streamId;
        this.producerId = producerId;
    }

}
