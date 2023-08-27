package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.room.RoomLeaveEvent;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 离开房间信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID"
    }
    {
        "roomId"  : "房间ID"
        "clientId": "离开终端ID"
    }
    """,
    flow = {
        "终端->信令服务-)终端",
        "终端-[关闭终端]>信令服务-)终端",
    }
)
public class RoomLeaveProtocol extends ProtocolRoomAdapter implements ApplicationListener<RoomLeaveEvent> {

    public static final String SIGNAL = "room::leave";
    
    public RoomLeaveProtocol() {
        super("离开房间信令", SIGNAL);
    }

    @Async
    @Override
    public void onApplicationEvent(RoomLeaveEvent event) {
        final Room   room   = event.getRoom();
        final Client client = event.getClient();
        final Map<String, String> body = Map.of(
            Constant.ROOM_ID,   room.getRoomId(),
            Constant.CLIENT_ID, client.getClientId()
        );
        room.broadcast(client, this.build(body));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            room.leave(client);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
