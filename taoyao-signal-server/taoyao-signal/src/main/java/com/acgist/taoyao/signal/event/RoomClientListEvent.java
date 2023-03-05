package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.party.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 房间终端列表事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class RoomClientListEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 终端
     */
    private final Client client;
    
    public RoomClientListEvent(Room room, Client client) {
        super(room);
        this.client = client;
    }

}
