package com.acgist.taoyao.signal.event.media;

import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.room.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 关闭通道事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class TransportCloseEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;

    /**
     * 通道ID
     */
    private final String transportId;
    
    public TransportCloseEvent(String transportId, Room room) {
        super(room);
        this.transportId = transportId;
    }
    
}
