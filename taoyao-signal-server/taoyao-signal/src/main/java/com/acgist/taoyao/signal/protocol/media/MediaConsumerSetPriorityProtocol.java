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
 * 设置消费者优先级信令
 * 
 * TODO：unsetPriority
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID",
        "consumerId": "消费者ID",
        "priority": 优先级（1~255）
    }
    """
)
public class MediaConsumerSetPriorityProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::consumer::set::priority";
    
    public MediaConsumerSetPriorityProtocol() {
        super("设置消费者优先级信令", SIGNAL);
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
