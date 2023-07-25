package com.acgist.taoyao.signal.party.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.media.MediaDataProducerCloseEvent;
import com.acgist.taoyao.signal.party.room.ClientWrapper;
import com.acgist.taoyao.signal.party.room.Room;

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
public class DataProducer extends OperatorAdapter {

    /**
     * 数据流ID
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
    private final Map<String, DataConsumer> dataConsumers;
    
    public DataProducer(String streamId, String producerId, Room room, ClientWrapper producerClient) {
        this.streamId       = streamId;
        this.producerId     = producerId;
        this.room           = room;
        this.producerClient = producerClient;
        this.dataConsumers  = new ConcurrentHashMap<>();
    }
    
    @Override
    public void close() {
        if(this.markClose()) {
            return;
        }
        log.info("关闭数据生产者：{} - {}", this.streamId, this.producerId);
        this.dataConsumers.values().forEach(DataConsumer::close);
        EventPublisher.publishEvent(new MediaDataProducerCloseEvent(this.producerId, this.room));
    }
    
    @Override
    public void remove() {
        log.info("移除数据生产者：{} - {}", this.streamId, this.producerId);
        this.room.getDataProducers().remove(this.producerId);
        this.producerClient.getDataProducers().remove(this.producerId);
    }
    
    @Override
    public void log() {
        log.info("当前数据生产者：{} - {}", this.streamId, this.producerId);
        this.dataConsumers.values().forEach(DataConsumer::log);
    }
    
}
