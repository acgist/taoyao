package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 创建房间信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "name": "房间名称",
        "passowrd": "房间密码",
        "mediaId": "媒体服务标识",
        "clientSize": "终端数量"
    }
    """,
    flow = "终端->服务端+)终端"
)
public class RoomCreateProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "room::create";
	
	public RoomCreateProtocol() {
		super("创建房间信令", SIGNAL);
	}

	@Override
    public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
        final String roomId = this.get(body, Constant.ROOM_ID);
        if (roomId != null && this.roomManager.room(roomId) != null) {
            throw MessageCodeException.of("房间已经存在");
        }
        // 创建房间
        final Room room = this.roomManager.create(
            this.get(body, Constant.NAME),
            this.get(body, Constant.PASSWORD),
            this.get(body, Constant.MEDIA_ID),
            message.cloneWithoutBody()
        );
        // 广播消息
        message.setBody(room.getStatus());
        this.clientManager.broadcast(message);
    }

}
