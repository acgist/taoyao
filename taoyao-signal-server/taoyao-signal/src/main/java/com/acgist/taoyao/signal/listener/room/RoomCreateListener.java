package com.acgist.taoyao.signal.listener.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.room.RoomCreateEvent;
import com.acgist.taoyao.signal.listener.RoomListenerAdapter;
import com.acgist.taoyao.signal.room.Room;

/**
 * 创建房间监听
 * 
 * @author acgist
 */
@EventListener
public class RoomCreateListener extends RoomListenerAdapter<RoomCreateEvent> {

	@Override
	public void onApplicationEvent(RoomCreateEvent event) {
		final Room room = this.roomManager.create(event.getSn());
		final Message message = event.getMessage();
		message.setBody(Map.of("id", room.getId()));
		this.clientSessionManager.broadcast(message);
	}

}
