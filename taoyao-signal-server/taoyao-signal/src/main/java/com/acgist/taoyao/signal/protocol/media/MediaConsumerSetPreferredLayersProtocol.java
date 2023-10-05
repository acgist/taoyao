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
 * 修改最佳空间层和时间层信令
 * 
 * 空间层（spatialLayer） ：分辨率
 * 时间层（temporalLayer）：帧率
 * 
 * 码率：数据大小和时间的比值
 * 
 * 注意：只有Simulcast和SVC视频消费者有效
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId"       : "房间ID",
        "consumerId"   : "消费者ID",
        "spatialLayer" : 最佳空间层,
        "temporalLayer": 最佳时间层
    }
    """,
    flow = "终端->信令服务->媒体服务"
)
public class MediaConsumerSetPreferredLayersProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::consumer::set::preferred::layers";
    
    public MediaConsumerSetPreferredLayersProtocol() {
        super("修改最佳空间层和时间层信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            mediaClient.push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }
    
}
