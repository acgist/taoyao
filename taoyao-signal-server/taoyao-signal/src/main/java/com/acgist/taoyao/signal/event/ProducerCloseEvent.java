package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.party.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 关闭生产者事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class ProducerCloseEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 生产者ID
     */
    private final String producerId;
    
    public ProducerCloseEvent(String producerId, Room room) {
        super(room);
        this.producerId = producerId;
    }

}
