package com.acgist.taoyao.signal.event.room;

import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.room.Room;

/**
 * 关闭房间事件
 * 
 * @author acgist
 */
public class RoomCloseEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public RoomCloseEvent(Room room) {
        super(room);
    }

}
