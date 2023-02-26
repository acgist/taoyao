package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.flute.media.Producer;
import com.acgist.taoyao.signal.flute.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 媒体生产事件
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
    
    public MediaProduceEvent(Room room, Producer producer) {
        super(room);
        this.producer = producer;
    }

}
