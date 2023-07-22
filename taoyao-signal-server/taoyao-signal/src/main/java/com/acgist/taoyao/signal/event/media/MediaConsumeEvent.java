package com.acgist.taoyao.signal.event.media;

import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 消费媒体事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaConsumeEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 生产者
     */
    private final Producer producer;
    /**
     * 消费者终端包装器
     */
    private final ClientWrapper clientWrapper;
    
    public MediaConsumeEvent(Room room, Producer producer) {
        super(room);
        this.producer      = producer;
        this.clientWrapper = null;
    }
    
    public MediaConsumeEvent(Room room, ClientWrapper clientWrapper) {
        super(room);
        this.producer      = null;
        this.clientWrapper = clientWrapper;
    }
    
}
