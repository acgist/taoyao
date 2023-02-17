package com.acgist.taoyao.signal.event;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.media.Room;

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
    
    public RoomEventAdapter(Message message, Room room) {
        this(Map.of(), message, room);
    }
    
    public RoomEventAdapter(Map<?, ?> body, Message message, Room room) {
        super(body, message, room);
        this.room = room;
    }

}
