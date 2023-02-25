package com.acgist.taoyao.signal.protocol.room;

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
import com.acgist.taoyao.signal.event.MediaClientRegisterEvent;
import com.acgist.taoyao.signal.flute.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 创建房间信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
    {
        "name": "房间名称",
        "passowrd": "房间密码",
        "mediaId": "媒体服务标识",
        "clientSize": "终端数量"
    }
    """,
    flow = "终端->服务端+)终端"
)
public class RoomCreateProtocol extends ProtocolClientAdapter implements ApplicationListener<MediaClientRegisterEvent> {

	public static final String SIGNAL = "room::create";
	
	public RoomCreateProtocol() {
		super("创建房间信令", SIGNAL);
	}

	@Async
	@Override
	public void onApplicationEvent(MediaClientRegisterEvent event) {
	    this.roomManager.recreate(event.getClient(), this.build());
	}

	@Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
	    if(clientType == ClientType.WEB) {
	        final String roomId = MapUtils.get(body, Constant.ROOM_ID);
	        if (roomId != null && this.roomManager.room(roomId) != null) {
	            log.info("房间已经存在：{}", roomId);
	            return;
	        }
	        // 创建房间
	        final Room room = this.roomManager.create(
	            MapUtils.get(body, Constant.NAME),
	            MapUtils.get(body, Constant.PASSWORD),
	            MapUtils.get(body, Constant.MEDIA_ID),
	            message.cloneWithoutBody()
            );
	        // 广播消息
	        message.setBody(room.getRoomStatus());
	        this.clientManager.broadcast(message);
	    } else {
	        this.logNoAdapter(clientType);
	    }
    }

}
