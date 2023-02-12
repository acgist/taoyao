package com.acgist.taoyao.signal.protocol;

import java.net.http.WebSocket;
import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.room.Room;

/**
 * 房间媒体服务信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolMediaRoomAdapter extends ProtocolMediaAdapter {

	protected ProtocolMediaRoomAdapter(String name, String signal) {
		super(name, signal);
	}
	
	@Override
	public void execute(Message message, WebSocket webSocket) {
		final Object body = message.getBody();
		if(body instanceof Map<?, ?> map) {
			this.execute(this.room(map), map, message, webSocket);
		} else {
			throw MessageCodeException.of("信令主体类型错误：" + message);
		}
	}
	
	@Override
	public void execute(String sn, Client client, Message message) {
		final Object body = message.getBody();
		if(body instanceof Map<?, ?> map) {
			this.execute(sn, this.room(map), map, client, message);
		} else {
			throw MessageCodeException.of("信令主体类型错误：" + message);
		}
	}
	
	/**
	 * @param map 参数
	 * 
	 * @return 房间
	 */
	protected Room room(Map<?, ?> map) {
		final Long roomId = this.roomId(map);
		final Room room = this.roomManager.room(roomId);
		if(room == null) {
			throw MessageCodeException.of("房间无效：" + roomId);
		}
		return room;
	}
	
	/**
	 * @param map 参数
	 * 
	 * @return 房间ID
	 */
	protected Long roomId(Map<?, ?> map) {
		final Object object = map.get(Constant.ROOM_ID);
		if(object == null) {
			return null;
		} else if(object instanceof Long value) {
			return value;
		}
		return Long.valueOf(object.toString());
	}
	
	/**
	 * 处理房间信令
	 * 
	 * @param room 房间
	 * @param body 消息
	 * @param message 信令消息
	 * @param webSocket WebSocket
	 */
	public abstract void execute(Room room, Map<?, ?> body, Message message, WebSocket webSocket);
	
	/**
	 * 处理终端信令
	 * 
	 * @param sn 终端标识
	 * @param room 房间
	 * @param body 消息
	 * @param client 终端
	 * @param message 信令消息
	 */
	public abstract void execute(String sn, Room room, Map<?, ?> body, Client client, Message message);
	
}
