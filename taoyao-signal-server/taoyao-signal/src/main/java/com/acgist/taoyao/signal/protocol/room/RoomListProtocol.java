package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.media.RoomManager;
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
            "name": "房间名称",
            "passowrd": "房间密码",
            "mediaId": "媒体服务标识",
            "clientSize": "终端数量"
        },
        ...
    ]
    """
)
public class RoomListProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "room::list";
	
	@Autowired
	private RoomManager roomManager;
	
	public RoomListProtocol() {
		super("房间列表信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		message.setBody(this.roomManager.status());
		client.push(message);
	}

}
