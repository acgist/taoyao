package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.media.TransportCloseEvent;
import com.acgist.taoyao.signal.party.media.Transport;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭通道信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    memo = "关闭通过回调实现所以不能同步响应",
    body = """
    {
        "roomId"     : "房间ID"
        "transportId": "通道ID"
    }
    """,
    flow = "终端->信令服务->媒体服务->信令服务->终端"
)
public class MediaTransportCloseProtocol extends ProtocolRoomAdapter implements ApplicationListener<TransportCloseEvent> {

    public static final String SIGNAL = "media::transport::close";
    
    public MediaTransportCloseProtocol() {
        super("关闭通道信令", SIGNAL);
    }
    
    @Async
    @Override
    public void onApplicationEvent(TransportCloseEvent event) {
        final Room room          = event.getRoom();
        final Client mediaClient = event.getMediaClient();
        final Map<String, Object> body = Map.of(
            Constant.ROOM_ID,      room.getRoomId(),
            Constant.TRANSPORT_ID, event.getTransportId()
        );
        mediaClient.push(this.build(body));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String transportId  = MapUtils.get(body, Constant.TRANSPORT_ID);
        final Transport transport = room.transport(transportId);
        if(transport == null) {
            log.debug("通道无效：{} - {}", transportId, clientType);
            return;
        }
        if(clientType.mediaClient()) {
            transport.close();
        } else if(clientType.mediaServer()) {
            transport.remove();
            transport.getClient().push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
