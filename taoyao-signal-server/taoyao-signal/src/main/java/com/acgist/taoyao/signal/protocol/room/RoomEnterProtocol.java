package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 进入房间信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "roomId": "房间标识"
        }
        """,
        """
        {
            "roomId": "房间标识",
            "clientId": "终端标识"
        }
        """
    },
    flow = "终端->服务端-)终端"
)
public class RoomEnterProtocol extends ProtocolRoomAdapter {

	public static final String SIGNAL = "room::enter";
	
	public RoomEnterProtocol() {
		super("进入房间信令", SIGNAL);
	}
	
	@Override
    public void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message) {
    }

	@Override
	public void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message) {
        final String password = this.get(body, Constant.PASSWORD);
        final String roomPassowrd = room.getPassword();
        if(roomPassowrd != null && !roomPassowrd.equals(password)) {
            throw MessageCodeException.of(MessageCode.CODE_3401, "密码错误");
        }
        final MediaClient mediaClient = room.getMediaClient();
        if(client.mediaClient() == null) {
            client.mediaClient(mediaClient);
        } else if(client.mediaClient() == mediaClient) {
        } else {
            throw MessageCodeException.of("不在相同媒体服务：" + mediaClient.mediaId());
        }
        // 进入房间
        room.enter(client);
        // 发送通知
        message.setBody(Map.of(
            Constant.ROOM_ID, room.getRoomId(),
            Constant.CLIENT_ID, clientId
        ));
        room.broadcast(message);
	}

}
