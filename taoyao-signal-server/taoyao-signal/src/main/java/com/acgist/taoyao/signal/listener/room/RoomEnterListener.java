package com.acgist.taoyao.signal.listener.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.event.room.RoomEnterEvent;
import com.acgist.taoyao.signal.listener.RoomListenerAdapter;
import com.acgist.taoyao.signal.room.Room;

/**
 * 进入房间监听
 * 
 * @author acgist
 */
@EventListener
public class RoomEnterListener extends RoomListenerAdapter<RoomEnterEvent> {

	@Override
	public void onApplicationEvent(RoomEnterEvent event) {
		final String sn = event.getSn();
		final String id = event.get("id");
		final Room room = this.roomManager.room(id);
		if(room == null) {
			throw MessageCodeException.of(MessageCode.CODE_3400, "无效房间");
		}
		room.addSn(sn);
		final Message message = event.getMessage();
		message.setBody(Map.of(
			"id", room.getId(),
			"sn", sn
		));
		// TODO：返回房间列表
		room.getSnList().stream()
		.filter(v -> !sn.equals(v))
		.forEach(v -> this.clientSessionManager.unicast(v, message));
	}

}
