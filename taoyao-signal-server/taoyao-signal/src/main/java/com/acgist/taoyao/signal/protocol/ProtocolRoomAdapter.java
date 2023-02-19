package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.property.Constant;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Room;

/**
 * 房间信令协议适配器
 * 
 * TODO：校验是否是房间内的用户权限
 * 
 * @author acgist
 */
public abstract class ProtocolRoomAdapter extends ProtocolMediaAdapter {

	protected ProtocolRoomAdapter(String name, String signal) {
		super(name, signal);
	}
	
	@Override
	public void execute(Map<?, ?> body, MediaClient mediaClient, Message message) {
		this.execute(this.room(body), body, mediaClient, message);
	}
	
	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		this.execute(clientId, this.room(body), body, client, message);
	}
	
	/**
	 * @param body 信令主体
	 * 
	 * @return 房间
	 */
	protected Room room(Map<?, ?> body) {
		final String roomId = this.get(body, Constant.ROOM_ID);
		final Room room = this.roomManager.room(roomId);
		if(room == null) {
			throw MessageCodeException.of("房间无效：" + roomId);
		}
		return room;
	}
	
	/**
	 * 处理房间信令
	 * 
	 * @param room 房间
	 * @param body 消息
	 * @param mediaClient 媒体服务终端
	 * @param message 信令消息
	 */
	public abstract void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message);
	
	/**
	 * 处理终端信令
	 * 
	 * @param clientId 终端标识
	 * @param room 房间
	 * @param body 消息
	 * @param client 终端
	 * @param message 信令消息
	 */
	public abstract void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message);
	
}
