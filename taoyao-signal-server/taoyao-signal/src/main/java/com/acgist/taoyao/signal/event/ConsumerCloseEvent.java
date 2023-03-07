package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.party.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 关闭消费者事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class ConsumerCloseEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 消费者ID
     */
    private final String consumerId;
    
    public ConsumerCloseEvent(String consumerId, Room room) {
        super(room);
        this.consumerId = consumerId;
    }

}
