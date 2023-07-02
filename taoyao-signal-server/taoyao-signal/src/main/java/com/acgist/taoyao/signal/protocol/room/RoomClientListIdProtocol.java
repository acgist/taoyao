package com.acgist.taoyao.signal.protocol.room;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Consumer;
import com.acgist.taoyao.signal.party.media.Kind;
import com.acgist.taoyao.signal.party.media.Producer;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.party.media.RoomClientId;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 房间终端ID信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "roomId": "房间ID",
        "clientId": "终端ID（可选）"
    }
    {
        ...
    }
    """,
    flow = "终端=>信令服务->终端",
    memo = "终端所有ID集合：消费者、生产者等等"
)
public class RoomClientListIdProtocol extends ProtocolRoomAdapter {

    public static final String SIGNAL = "room::client::list::id";
    
    public RoomClientListIdProtocol() {
        super("房间终端ID信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final String queryClientId = MapUtils.get(body, Constant.CLIENT_ID, clientId);
        final ClientWrapper clientWrapper = room.clientWrapper(queryClientId);
        final RoomClientId roomClientId = new RoomClientId();
        roomClientId.setRoomId(room.getRoomId());
        roomClientId.setClientId(queryClientId);
        clientWrapper.getDataProducers().keySet().forEach(roomClientId.getDataProducers()::add);
        clientWrapper.getDataConsumers().keySet().forEach(roomClientId.getDataConsumers()::add);
        clientWrapper.getProducers().values().stream()
            .filter(v -> v.getKind() == Kind.AUDIO)
            .map(Producer::getProducerId)
            .forEach(roomClientId.getAudioProducers()::add);
        clientWrapper.getProducers().values().stream()
            .filter(v -> v.getKind() == Kind.VIDEO)
            .map(Producer::getProducerId)
            .forEach(roomClientId.getVideoProducers()::add);
        clientWrapper.getConsumers().values().stream()
            .filter(v -> v.getKind() == Kind.AUDIO)
            .map(Consumer::getConsumerId)
            .forEach(roomClientId.getAudioConsumers()::add);
        clientWrapper.getConsumers().values().stream()
            .filter(v -> v.getKind() == Kind.VIDEO)
            .map(Consumer::getConsumerId)
            .forEach(roomClientId.getVideoConsumers()::add);
        message.setBody(roomClientId);
        client.push(message);
    }
    
}
