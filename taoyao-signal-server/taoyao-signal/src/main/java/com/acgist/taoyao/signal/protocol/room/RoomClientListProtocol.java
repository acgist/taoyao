package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.flute.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 房间终端列表信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    flow = "终端=>信令服务->终端"
)
public class RoomClientListProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "room::client::list";
    
    public RoomClientListProtocol() {
        super("房间终端列表信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        message.setBody(room.clientStatus());
        client.push(message);
    }
    
}
