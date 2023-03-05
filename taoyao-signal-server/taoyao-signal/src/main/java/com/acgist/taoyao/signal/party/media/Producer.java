package com.acgist.taoyao.signal.party.media;

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
    private final ClientWrapper produceClient;
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
    
    public Producer(ClientWrapper produceClient, String kind, String streamId, String producerId) {
        this.produceClient = produceClient;
        this.kind = Kind.of(kind);
        this.streamId = streamId;
        this.producerId = producerId;
        this.consumers = new ConcurrentHashMap<>();
    }
    
    /**
     * 删除消费者
     * 
     * @param consumer 消费者
     */
    public void remove(ClientWrapper consumer) {
        this.consumers.entrySet().stream()
        .filter(v -> v.getValue().getConsumeClient() == consumer)
        .map(Map.Entry::getKey)
        .forEach(this.consumers::remove);
        // TODO：资源释放
    }
    
}
