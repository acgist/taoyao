package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 房间列表信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    [
        {
            "name"         : "房间名称",
            "passowrd"     : "房间密码",
            "clientSize"   : "终端数量",
            "mediaClientId": "媒体服务标识"
        },
        ...
    ]
    """,
    flow = "终端=>信令服务"
)
public class RoomListProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "room::list";
    
    public RoomListProtocol() {
        super("房间列表信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        message.setBody(this.roomManager.getStatus());
        client.push(message);
    }

}
