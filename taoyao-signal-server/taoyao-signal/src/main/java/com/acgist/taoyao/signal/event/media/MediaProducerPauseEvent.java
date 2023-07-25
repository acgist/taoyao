package com.acgist.taoyao.signal.event.media;

import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.room.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 暂停生产者事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaProducerPauseEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 生产者ID
     */
    private final String producerId;
    
    public MediaProducerPauseEvent(String producerId, Room room) {
        super(room);
        this.producerId = producerId;
    }
    
}
