package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 创建房间信令
 * 
 * @author acgist
 */
@Protocol
public class RoomCreateProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "room::create";
	
	public RoomCreateProtocol() {
		super("创建房间信令", SIGNAL);
	}

	@Override
    public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
        final Long roomId = this.getLong(body, Constant.ROOM_ID);
        if (roomId != null && this.roomManager.room(roomId) != null) {
            throw MessageCodeException.of("房间已经存在");
        }
        // 创建房间
        final Room room = this.roomManager.create(
            clientId,
            this.get(body, Constant.NAME),
            this.get(body, Constant.PASSWORD),
            this.get(body, Constant.MEDIA_ID),
            message.cloneWidthoutBody()
        );
        // 广播消息
        message.setBody(room.getStatus());
        this.clientManager.broadcast(message);
    }

}
