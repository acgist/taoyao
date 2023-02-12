package com.acgist.taoyao.signal.listener.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.room.RoomEnterEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.room.Room;

/**
 * 进入房间监听
 * 
 * @author acgist
 */
@EventListener
public class RoomEnterListener extends ApplicationListenerAdapter<RoomEnterEvent> {

	@Override
	public void onApplicationEvent(RoomEnterEvent event) {
		final Long id = event.getId();
		final String sn = event.getSn();
		final String password = event.getPassword();
		final Room room = this.roomManager.room(id);
		if(room == null) {
			throw MessageCodeException.of(MessageCode.CODE_3400, "无效房间");
		}
		final String roomPassowrd = room.getPassword();
		if(roomPassowrd != null && !roomPassowrd.equals(password)) {
			throw MessageCodeException.of(MessageCode.CODE_3401, "密码错误");
		}
		final Client client = event.getClient();
		final MediaClient mediaClient = room.getMediaClient();
		if(client.mediaClient() == null) {
			client.mediaClient(mediaClient);
		} else if(client.mediaClient() == mediaClient) {
		} else {
			throw MessageCodeException.of("不在相同媒体服务：" + mediaClient.name());
		}
		// 进入房间
		room.enter(client);
		// 发送通知
		final Message message = event.getMessage();
		message.setBody(Map.of(
			Constant.ID, room.getId(),
			Constant.SN, sn
		));
		room.broadcast(message);
	}

}
