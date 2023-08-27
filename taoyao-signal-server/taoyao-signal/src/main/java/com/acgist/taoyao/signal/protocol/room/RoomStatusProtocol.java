package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 房间状态信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "roomId": "房间ID"
        }
        """,
        """
        {
            "name"         : "房间名称",
            "passowrd"     : "房间密码",
            "clientSize"   : "终端数量",
            "mediaClientId": "媒体服务标识"
        }
        """
    },
    flow = "终端=>信令服务"
)
public class RoomStatusProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "room::status";
    
    public RoomStatusProtocol() {
        super("房间状态信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        message.setBody(room.getRoomStatus());
        client.push(message);
    }
    
}
