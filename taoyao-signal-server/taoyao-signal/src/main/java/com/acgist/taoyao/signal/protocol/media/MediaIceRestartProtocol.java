package com.acgist.taoyao.signal.protocol.media;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;

/**
 * 媒体重启ICE信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "roomId": "房间标识",
            "transportId": "通道标识"
        }
        """,
        """
        {
            "roomId": "房间标识",
            "transportId": "通道标识",
            "iceParameters": "iceParameters"
        }
        """
    },
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaIceRestartProtocol {

    public static final String SIGNAL = "media::ice::restart";
    
}
