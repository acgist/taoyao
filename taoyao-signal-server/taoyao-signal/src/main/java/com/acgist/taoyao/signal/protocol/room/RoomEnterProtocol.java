package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.flute.media.ClientWrapper;
import com.acgist.taoyao.signal.flute.media.ClientWrapper.SubscribeType;
import com.acgist.taoyao.signal.flute.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 进入房间信令
 * 
 * @author acgist
 */
@Slf4j
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
	
	private final RoomClientListProtocol roomClientListProtocol;
	
	public RoomEnterProtocol(RoomClientListProtocol roomClientListProtocol) {
		super("进入房间信令", SIGNAL);
		this.roomClientListProtocol = roomClientListProtocol;
	}

	@Override
	public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String password = MapUtils.get(body, Constant.PASSWORD);
        final String subscribeType = MapUtils.get(body, Constant.SUBSCRIBE_TYPE);
        final Object rtpCapabilities = MapUtils.get(body, Constant.RTP_CAPABILITIES);
        final Object sctpCapabilities = MapUtils.get(body, Constant.SCTP_CAPABILITIES);
        final String roomPassowrd = room.getPassword();
        if(roomPassowrd != null && !roomPassowrd.equals(password)) {
            throw MessageCodeException.of(MessageCode.CODE_3401, "密码错误");
        }
        // 进入房间
        final ClientWrapper clientWrapper = room.enter(client);
        clientWrapper.setSubscribeType(SubscribeType.of(subscribeType));
        clientWrapper.setRtpCapabilities(rtpCapabilities);
        clientWrapper.setSctpCapabilities(sctpCapabilities);
        // 发送通知
        message.setBody(Map.of(
            Constant.ROOM_ID, room.getRoomId(),
            Constant.CLIENT_ID, clientId
        ));
        room.broadcast(message);
        log.info("进入房间：{} - {}", clientId, room.getRoomId());
        // 推送房间用户信息
        final Message roomClientListMessage = this.roomClientListProtocol.build();
        roomClientListMessage.setBody(room.clientStatus());
        client.push(roomClientListMessage);
	}

}
