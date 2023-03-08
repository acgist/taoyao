package com.acgist.taoyao.signal.party.media;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.media.MediaProducerCloseEvent;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 生产者
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Producer implements Closeable {
    
    /**
     * 是否关闭
     */
    private volatile boolean close = false;
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
     * 房间
     */
    private final Room room;
    /**
     * 生产者终端
     */
    private final ClientWrapper producerClient;
    /**
     * 消费者
     * 其他终端消费当前终端的消费者
     */
    private final Map<String, Consumer> consumers;
    
    public Producer(String kind, String streamId, String producerId, Room room, ClientWrapper produceClient) {
        this.kind = Kind.of(kind);
        this.streamId = streamId;
        this.producerId = producerId;
        this.room = room;
        this.producerClient = produceClient;
        this.consumers = new ConcurrentHashMap<>();
    }

    /**
     * 删除消费者
     * 
     * @param consumerId 消费者ID
     */
    public void remove(String consumerId) {
        this.consumers.remove(consumerId);
    }
    
    @Override
    public void close() {
        if(this.close) {
            return;
        }
        this.close = true;
        log.info("关闭生产者：{}", this.producerId);
        this.consumers.forEach((k, v) -> v.close());
        this.producerClient.getProducers().remove(this.producerId);
        EventPublisher.publishEvent(new MediaProducerCloseEvent(this.producerId, this.room));
    }
    
}
