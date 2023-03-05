package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.party.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 进入房间事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class RoomEnterEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 终端
     */
    private final Client client;
    
    public RoomEnterEvent(Room room, Client client) {
        super(room);
        this.client = client;
    }

}
