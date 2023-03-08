package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.room.RoomEnterEvent;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.ClientWrapper.SubscribeType;
import com.acgist.taoyao.signal.party.media.Room;
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
            "roomId": "房间ID",
            "password": "房间密码（选填）"
        }
        """,
        """
        {
            "roomId": "房间标识",
            "clientId": "终端标识"
        }
        """
    },
    flow = "终端->信令服务-)终端"
)
public class RoomEnterProtocol extends ProtocolRoomAdapter {

	public static final String SIGNAL = "room::enter";
	
	public RoomEnterProtocol() {
		super("进入房间信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
	    if(clientType.mediaClient()) {
	        this.enter(clientId, room, client, message, body);
	    } else {
	        this.logNoAdapter(clientType);
	    }
	}

	/**
	 * 终端进入
	 * 
	 * @param clientId 终端ID
	 * @param room 房间
	 * @param client 终端
	 * @param message 消息
	 * @param body 消息主体
	 */
    private void enter(String clientId, Room room, Client client, Message message, Map<String, Object> body) {
        final String password = MapUtils.get(body, Constant.PASSWORD);
        final String roomPassowrd = room.getPassword();
        if(StringUtils.isNotEmpty(roomPassowrd) && !roomPassowrd.equals(password)) {
            throw MessageCodeException.of(MessageCode.CODE_3401, "密码错误");
        }
        final String subscribeType = MapUtils.get(body, Constant.SUBSCRIBE_TYPE);
        final Object rtpCapabilities = MapUtils.get(body, Constant.RTP_CAPABILITIES);
        final Object sctpCapabilities = MapUtils.get(body, Constant.SCTP_CAPABILITIES);
        // 进入房间
        final ClientWrapper clientWrapper = room.enter(client);
        // 配置参数
        clientWrapper.setSubscribeType(SubscribeType.of(subscribeType));
        clientWrapper.setRtpCapabilities(rtpCapabilities);
        clientWrapper.setSctpCapabilities(sctpCapabilities);
        // 发送通知
        message.setBody(Map.of(
            Constant.ROOM_ID, room.getRoomId(),
            Constant.CLIENT_ID, clientId,
            Constant.STATUS, client.status()
        ));
        room.broadcast(message);
        // 进入房间事件
        this.publishEvent(new RoomEnterEvent(room, client));
    }

}
