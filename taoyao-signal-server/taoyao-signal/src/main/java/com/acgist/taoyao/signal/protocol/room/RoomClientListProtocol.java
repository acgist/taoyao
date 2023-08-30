package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.room.RoomEnterEvent;
import com.acgist.taoyao.signal.party.room.Room;
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
                "ip"             : "终端IP",
                "name"           : "终端名称",
                "clientId"       : "终端ID",
                "clientType"     : "终端类型",
                "latitude"       : 纬度,
                "longitude"      : 经度,
                "humidity"       : 湿度,
                "temperature"    : 温度,
                "signal"         : 信号强度（0~100）,
                "battery"        : 电池电量（0~100）,
                "alarming"       : 是否发生告警（true|false）,
                "charging"       : 是否正在充电（true|false）,
                "clientRecording": 是否正在录像（true|false）,
                "serverRecording": 是否正在录像（true|false）,
                "lastHeartbeat"  : "最后心跳时间",
                "status"         : {更多状态},
                "config"         : {更多配置}
            },
            ...
        ]
        """
    },
    flow = {
        "终端=>信令服务",
        "终端=[进入房间]>信令服务->终端",
    }
)
public class RoomClientListProtocol extends ProtocolRoomAdapter implements ApplicationListener<RoomEnterEvent> {

    public static final String SIGNAL = "room::client::list";
    
    public RoomClientListProtocol() {
        super("房间终端列表信令", SIGNAL);
    }

    @Async
    @Override
    public void onApplicationEvent(RoomEnterEvent event) {
        final Room room = event.getRoom();
        final Client client = event.getClient();
        client.push(this.build(Map.of(
            Constant.ROOM_ID, room.getRoomId(),
            Constant.CLIENTS, room.getClientStatus()
        )));
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        message.setBody(Map.of(
            Constant.ROOM_ID, room.getRoomId(),
            Constant.CLIENTS, room.getClientStatus()
        ));
        client.push(message);
    }
    
}
