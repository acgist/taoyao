package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 房间广播信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID",
        ...
    }
    """,
    flow = "终端->信令服务-)终端"
)
public class RoomBroadcastProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "room::broadcast";
    
    public RoomBroadcastProtocol() {
        super("房间广播信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        room.broadcast(client, message);
    }

}
