package com.acgist.taoyao.signal.event;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.party.room.Room;

import lombok.Getter;

/**
 * 房间事件适配器
 * 
 * @author acgist
 */
@Getter
public class RoomEventAdapter extends ApplicationEventAdapter {

    private static final long serialVersionUID = 1L;

    /**
     * 房间
     */
    private final Room room;
    
    public RoomEventAdapter(Room room) {
        this(room, null, null);
    }
    
    public RoomEventAdapter(Room room, Message message) {
        this(room, message, null);
    }
    
    public RoomEventAdapter(Room room, Message message, Map<String, Object> body) {
        super(room, message, body);
        this.room = room;
    }
    
    /**
     * @return 房间媒体服务终端
     */
    public Client getMediaClient() {
        return this.room.getMediaClient();
    }
    
}
