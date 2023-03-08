package com.acgist.taoyao.signal.event.room;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ClientEventAdapter;

/**
 * 创建房间事件
 * 
 * @author acgist
 */
public class RoomCreateEvent extends ClientEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public RoomCreateEvent(Client client) {
        super(client);
    }

}
