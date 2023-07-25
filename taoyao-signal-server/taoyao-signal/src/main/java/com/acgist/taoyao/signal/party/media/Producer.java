package com.acgist.taoyao.signal.party.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.media.MediaProducerCloseEvent;
import com.acgist.taoyao.signal.event.media.MediaProducerPauseEvent;
import com.acgist.taoyao.signal.event.media.MediaProducerResumeEvent;
import com.acgist.taoyao.signal.party.room.ClientWrapper;
import com.acgist.taoyao.signal.party.room.Room;

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
public class Producer extends OperatorAdapter {
    
    /**
     * 媒体类型
     */
    private final Kind kind;
    /**
     * 媒体流ID
     */
    private final String streamId;
    /**
     * 生产者ID
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
    
    public Producer(String kind, String streamId, String producerId, Room room, ClientWrapper producerClient) {
        this.kind           = Kind.of(kind);
        this.streamId       = streamId;
        this.producerId     = producerId;
        this.room           = room;
        this.producerClient = producerClient;
        this.consumers      = new ConcurrentHashMap<>();
    }

    @Override
    public void close() {
        if(this.markClose()) {
            return;
        }
        log.info("关闭生产者：{} - {}", this.streamId, this.producerId);
        this.consumers.values().forEach(Consumer::close);
        EventPublisher.publishEvent(new MediaProducerCloseEvent(this.producerId, this.room));
    }
    
    @Override
    public void remove() {
        log.info("移除生产者：{} - {}", this.streamId, this.producerId);
        this.room.getProducers().remove(this.producerId);
        this.producerClient.getProducers().remove(this.producerId);
    }
    
    @Override
    public void pause() {
        log.debug("暂停生产者：{} - {}", this.streamId, this.producerId);
        EventPublisher.publishEvent(new MediaProducerPauseEvent(this.producerId, this.room));
    }
    
    @Override
    public void resume() {
        log.debug("恢复生产者：{} - {}", this.streamId, this.producerId);
        EventPublisher.publishEvent(new MediaProducerResumeEvent(this.producerId, this.room));
    }
    
    @Override
    public void log() {
        log.info("当前生产者：{} - {} - {}", this.streamId, this.producerId, this.kind);
        this.consumers.values().forEach(Consumer::log);
    }
    
}
