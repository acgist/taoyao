package com.acgist.taoyao.signal.listener.room;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.event.room.RoomCreateEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.room.Room;

/**
 * 创建房间监听
 * 
 * @author acgist
 */
@EventListener
public class RoomCreateListener extends ApplicationListenerAdapter<RoomCreateEvent> {

	@Override
	public void onApplicationEvent(RoomCreateEvent event) {
		final Long id = event.getId();
		if(id != null && this.roomManager.room(id) != null) {
			throw MessageCodeException.of("房间已经存在");
		}
		// 广播消息
		final Message message = event.getMessage();
		// 创建房间
		final Room room = this.roomManager.create(
			event.getSn(),
			event.getName(),
			event.getPassword(),
			event.getMediaName(),
			message.cloneWidthoutBody()
		);
		// 进入房间
		room.enter(event.getClient());
		message.setBody(room.getStatus());
		this.clientManager.broadcast(message);
	}

}
