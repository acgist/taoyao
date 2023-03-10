package com.acgist.taoyao.signal.party.media;

import com.acgist.taoyao.signal.event.EventPublisher;
import com.acgist.taoyao.signal.event.media.MediaDataConsumerCloseEvent;

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
public class DataConsumer extends OperatorAdapter {

    /**
     * 数据流ID
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
    private final DataProducer dataProducer;
    /**
     * 消费者终端
     */
    private final ClientWrapper consumerClient;
    
    public DataConsumer(String streamId, String consumerId, Room room, DataProducer dataProducer, ClientWrapper consumerClient) {
        this.streamId = streamId;
        this.consumerId = consumerId;
        this.room = room;
        this.dataProducer = dataProducer;
        this.consumerClient = consumerClient;
    }
    
    @Override
    public void close() {
        if(this.markClose()) {
            return;
        }
        log.info("关闭数据消费者：{} - {}", this.streamId, this.consumerId);
        EventPublisher.publishEvent(new MediaDataConsumerCloseEvent(this.consumerId, this.room));
    }
    
    @Override
    public void remove() {
        log.info("移除数据消费者：{} - {}", this.streamId, this.consumerId);
        this.room.getDataProducers().remove(this.consumerId);
        this.dataProducer.getDataConsumers().remove(this.consumerId);
        this.consumerClient.getDataConsumers().remove(this.consumerId);
    }
    
    @Override
    public void log() {
        log.debug("当前数据消费者：{} - {}", this.consumerId, this.streamId);
    }
    
}
