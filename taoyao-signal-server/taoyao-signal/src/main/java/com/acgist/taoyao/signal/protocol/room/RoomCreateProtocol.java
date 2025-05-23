package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.room.MediaServerRegisterEvent;
import com.acgist.taoyao.signal.party.room.Room;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 创建房间信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "name"         : "房间名称",
            "passowrd"     : "房间密码（选填）",
            "mediaClientId": "媒体服务ID"
        }
        """,
        """
        {
            "roomId"       : "房间ID",
            "name"         : "房间名称",
            "clientSize"   : "终端数量",
            "mediaClientId": "媒体服务ID"
        }
        """
    },
    flow = {
        "终端=>信令服务->媒体服务",
        "终端->信令服务->媒体服务->信令服务-)终端"
    }
)
public class RoomCreateProtocol extends ProtocolClientAdapter implements ApplicationListener<MediaServerRegisterEvent> {

    public static final String SIGNAL = "room::create";
    
    public RoomCreateProtocol() {
        super("创建房间信令", SIGNAL);
    }

    @Async
    @Override
    public void onApplicationEvent(@NonNull MediaServerRegisterEvent event) {
        this.roomManager.recreate(event.getClient(), this.build());
    }

    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        if(clientType.isClient()) {
            final Room room = this.roomManager.create(
                MapUtils.get(body, Constant.NAME),
                MapUtils.get(body, Constant.PASSWORD),
                MapUtils.get(body, Constant.MEDIA_CLIENT_ID),
                message.cloneWithoutBody()
            );
            message.setBody(room.getRoomStatus());
            client.push(message);
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
