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
import com.acgist.taoyao.signal.event.RoomLeaveEvent;
import com.acgist.taoyao.signal.party.media.Room;
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
        this.leave(event.getRoom(), event.getClient());
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        // 离开房间后会发布事件
        room.leave(client);
    }

    /**
     * 离开房间
     * 
     * @param room 房间
     * @param client 终端
     */
    private void leave(Room room, Client client) {
        final Message leaveMessage = this.build(Map.of(Constant.CLIENT_ID, client.clientId()));
        room.broadcast(client, leaveMessage);
    }

}
