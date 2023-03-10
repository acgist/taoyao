package com.acgist.taoyao.signal.event.media;

import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 关闭数据消费者事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaDataConsumerCloseEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 消费者ID
     */
    private final String consumerId;
    
    public MediaDataConsumerCloseEvent(String consumerId, Room room) {
        super(room);
        this.consumerId = consumerId;
    }

}
