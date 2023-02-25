package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;
import com.acgist.taoyao.signal.terminal.media.Room;

/**
 * 关闭房间信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    flow = "终端->信令服务->媒体服务->信令服务+)终端"
)
public class RoomCloseProtocol extends ProtocolRoomAdapter {

    private static final String SIGNAL = "room::close";
    
    public RoomCloseProtocol() {
        super("关闭房间信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        room.close();
        this.clientManager.broadcast(message);
    }
    
}
