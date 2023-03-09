package com.acgist.taoyao.signal.party.media;

import java.io.Closeable;

import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.media.MediaConsumerCloseEvent;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 消费者
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Consumer implements Closeable {

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
     * 消费者标识
     */
    private final String consumerId;
    /**
     * 房间
     */
    private final Room room;
    /**
     * 生产者
     */
    private final Producer producer;
    /**
     * 消费者终端
     */
    private final ClientWrapper consumerClient;
    
    public Consumer(String kind, String streamId, String consumerId, Room room, Producer producer, ClientWrapper consumerClient) {
        this.kind = Kind.of(kind);
        this.streamId = streamId;
        this.consumerId = consumerId;
        this.room = room;
        this.producer = producer;
        this.consumerClient = consumerClient;
    }

    @Override
    public void close() {
        if(this.close) {
            return;
        }
        this.close = true;
        log.info("关闭消费者：{} - {}", this.streamId, this.consumerId);
        this.getProducer().remove(this.consumerId);
        this.consumerClient.getConsumers().remove(this.consumerId);
        EventPublisher.publishEvent(new MediaConsumerCloseEvent(this.consumerId, this.room));
    }
    
    /**
     * 记录日志
     */
    public void log() {
        log.debug("当前消费者：{} - {} - {}", this.consumerId, this.kind, this.streamId);
    }
    
}
