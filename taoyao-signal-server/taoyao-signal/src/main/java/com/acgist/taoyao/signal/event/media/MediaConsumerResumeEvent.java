package com.acgist.taoyao.signal.event.media;

import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.room.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 恢复消费者信令
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaConsumerResumeEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 消费者ID
     */
    private final String consumerId;
    
    public MediaConsumerResumeEvent(String consumerId, Room room) {
        super(room);
        this.consumerId = consumerId;
    }
    
}
