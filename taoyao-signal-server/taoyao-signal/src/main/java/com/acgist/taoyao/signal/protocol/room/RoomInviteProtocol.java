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
 * 邀请终端信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId"  : "房间ID",
        "clientId": "终端ID",
        "password": "密码（选填）"
    }
    """,
    flow = "终端->信令服务->终端"
)
public class RoomInviteProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "room::invite";
    
    public RoomInviteProtocol() {
        super("邀请终端信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.mediaClient()) {
            final String inviteClientId = MapUtils.get(body, Constant.CLIENT_ID);
            body.put(Constant.PASSWORD, room.getPassword());
            this.clientManager.unicast(inviteClientId, message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
