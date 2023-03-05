package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 生产媒体事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaProduceEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 生产者
     */
    private final Producer producer;
    /**
     * 消费者
     */
    private final ClientWrapper clientWrapper;
    
    public MediaProduceEvent(Room room, Producer producer) {
        super(room);
        this.producer = producer;
        this.clientWrapper = null;
    }
    
    public MediaProduceEvent(Room room, ClientWrapper clientWrapper) {
        super(room);
        this.producer = null;
        this.clientWrapper = clientWrapper;
    }
    
}
