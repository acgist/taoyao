package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 房间终端列表信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "roomId": "房间ID"
        }
        """,
        """
        [
            {
                "ip": "终端IP",
                "name": "终端名称",
                "clientId": "终端ID",
                "clientType": "终端类型",
                "latitude": 纬度,
                "longitude": 经度,
                "humidity": 湿度,
                "temperature": 温度,
                "signal": 信号强度（0~100）,
                "battery": 电池电量（0~100）,
                "alarming": 是否发生告警（true|false）,
                "charging": 是否正在充电（true|false）,
                "recording": 是否正在录像（true|false）,
                "lastHeartbeat": "最后心跳时间",
                "status": {更多状态},
                "config": {更多配置}
            },
            ...
        ]
        """
    },
    flow = "终端=>信令服务->终端"
)
public class RoomClientListProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "room::client::list";
    
    public RoomClientListProtocol() {
        super("房间终端列表信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        message.setBody(room.clientStatus());
        client.push(message);
    }
    
}
