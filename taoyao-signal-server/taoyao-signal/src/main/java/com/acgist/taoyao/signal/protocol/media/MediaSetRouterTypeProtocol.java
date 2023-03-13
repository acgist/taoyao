package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 设置路由类型信令
 * 注意：不会添加移除消费者生产者，只会暂停恢复操作。
 * 
 * @author acgist
 */
public class MediaSetRouterTypeProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "media::set::router::type";
    
    public MediaSetRouterTypeProtocol() {
        super("设置路由类型信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        if(clientType.mediaClient()) {
            // TODO：路由类型
        } else {
            this.logNoAdapter(clientType);
        }
    }

}
