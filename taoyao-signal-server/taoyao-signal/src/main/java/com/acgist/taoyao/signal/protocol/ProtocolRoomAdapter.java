package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.flute.media.Room;

/**
 * 房间信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolRoomAdapter extends ProtocolClientAdapter {

	protected ProtocolRoomAdapter(String name, String signal) {
		super(name, signal);
	}
	
	@Override
	public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, Constant.ROOM_ID);
        final Room room = this.roomManager.room(roomId);
        if(room == null) {
            throw MessageCodeException.of("无效房间：" + roomId);
        }
        // TODO：验证用户是否在房间里面
		this.execute(clientId, clientType, room, client, room.getMediaClient(), message, body);
	}
	
	/**
	 * 处理终端房间信令
	 * 
	 * @param clientId 终端标识
	 * @param clientType 终端类型
	 * @param room 房间
	 * @param client 终端
	 * @param mediaClient 媒体服务终端
	 * @param message 消息
	 * @param body 消息主体
	 */
	public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
	}
	
}
