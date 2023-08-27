package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 踢出房间信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    memo = "终端收到信令以后调用离开房间信令离开房间，没有实现强制在服务端提出。",
    body = """
    {
        "roomId"  : "房间ID",
        "clientId": "终端ID"
    }
    """,
    flow = "终端->信令服务->终端"
)
public class RoomExpelProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "room::expel";
    
    public RoomExpelProtocol() {
        super("踢出房间信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            final String expelClientId = MapUtils.get(body, Constant.CLIENT_ID);
            room.unicast(expelClientId, message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
