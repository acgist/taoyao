package com.acgist.taoyao.signal.event;

import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.terminal.media.Producer;
import com.acgist.taoyao.signal.terminal.media.Room;

import lombok.Getter;
import lombok.Setter;

/**
 * 媒体生产事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaProduceEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    private final Client client;
    private final Producer producer;
    
    public MediaProduceEvent(Room room, Client client, Producer producer) {
        super(room);
        this.client = client;
        this.producer = producer;
    }

}
