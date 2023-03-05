package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 路由RTP协商信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "roomId": "房间标识"
        }
        """,
        """
        {
            "codec": "编码解码",
            "headerExtensions": "扩展"
        }
        """
    },
    flow = { "终端->信令服务->媒体服务->信令服务->终端"}
)
public class MediaRouterRtpCapabilitiesProtocol extends ProtocolRoomAdapter {

	public static final String SIGNAL = "media::router::rtp::capabilities";
	
	public MediaRouterRtpCapabilitiesProtocol() {
		super("路由RTP协商信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
	    if(clientType == ClientType.WEB || clientType == ClientType.CAMERA) {
	        client.push(room.request(message));
	    } else {
	        this.logNoAdapter(clientType);
	    }
	}

}
