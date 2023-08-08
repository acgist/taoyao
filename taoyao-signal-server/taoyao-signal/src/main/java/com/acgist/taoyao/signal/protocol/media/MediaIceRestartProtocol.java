package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 重启ICE信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "roomId"     : "房间标识",
            "transportId": "通道标识"
        }
        """,
        """
        {
            "roomId"       : "房间标识",
            "transportId"  : "通道标识",
            "iceParameters": "iceParameters"
        }
        """
    },
    flow = "终端=>信令服务->媒体服务"
)
public class MediaIceRestartProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::ice::restart";
    
    public MediaIceRestartProtocol() {
        super("重启ICE信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.mediaClient()) {
            client.push(mediaClient.request(message));
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
