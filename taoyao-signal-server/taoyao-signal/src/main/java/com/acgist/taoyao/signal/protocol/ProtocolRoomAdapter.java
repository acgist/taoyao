package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.room.Room;

/**
 * 房间信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolRoomAdapter extends ProtocolClientAdapter {

    protected ProtocolRoomAdapter(String name, String signal) {
        super(name, signal);
    }
    
    @Override
    public boolean authenticate(Client client, Message message) {
        final Map<String, Object> body = message.body();
        final String roomId = MapUtils.get(body, Constant.ROOM_ID);
        final Room   room   = this.roomManager.getRoom(roomId);
        if(room == null) {
            throw MessageCodeException.of("无效房间：" + roomId);
        }
        if(!room.authenticate(client)) {
            throw MessageCodeException.of("终端没有房间权限：" + client.getClientId());
        }
        return true;
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, Constant.ROOM_ID);
        final Room   room   = this.roomManager.getRoom(roomId);
        if(room == null) {
            throw MessageCodeException.of("无效房间：" + roomId);
        }
        this.execute(clientId, clientType, room, client, room.getMediaClient(), message, body);
    }

    /**
     * 处理终端房间信令
     * 
     * @param clientId    终端ID
     * @param clientType  终端类型
     * @param room        房间
     * @param client      终端
     * @param mediaClient 媒体服务终端
     * @param message     信令消息
     * @param body        消息主体
     */
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
    }
    
}
