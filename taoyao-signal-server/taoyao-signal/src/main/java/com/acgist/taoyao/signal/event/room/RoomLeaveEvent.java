package com.acgist.taoyao.signal.event.room;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.room.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 离开房间事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class RoomLeaveEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;

    /**
     * 离开终端
     */
    private final Client client;
    
    public RoomLeaveEvent(Room room, Client client) {
        super(room);
        this.client = client;
    }

}
