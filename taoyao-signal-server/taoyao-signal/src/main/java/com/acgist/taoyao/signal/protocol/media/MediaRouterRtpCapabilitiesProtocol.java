package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.MediaClient;
import com.acgist.taoyao.signal.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 路由RTP能力信令
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
		super("路由RTP能力信令", SIGNAL);
	}

	@Override
	public void execute(Room room, Map<?, ?> body, MediaClient mediaClient, Message message) {
		// 忽略
	}

	@Override
	public void execute(String clientId, Room room, Map<?, ?> body, Client client, Message message) {
		client.push(room.request(message));
	}

}
