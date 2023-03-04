package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 关闭房间信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID"
    }
    """,
    flow = "终端->信令服务->媒体服务->信令服务+)终端"
)
public class RoomCloseProtocol extends ProtocolRoomAdapter {

    private static final String SIGNAL = "room::close";
    
    public RoomCloseProtocol() {
        super("关闭房间信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType == ClientType.WEB) {
            mediaClient.push(this.build(Map.of(Constant.ROOM_ID, room.getRoomId())));
        } else if(clientType == ClientType.MEDIA) {
            room.close();
            room.broadcast(message);
            // TODO：移除
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
