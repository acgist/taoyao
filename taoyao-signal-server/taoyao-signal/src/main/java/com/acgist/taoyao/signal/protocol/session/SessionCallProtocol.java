package com.acgist.taoyao.signal.protocol.session;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.session.Session;
import com.acgist.taoyao.signal.protocol.ProtocolSessionAdapter;

/**
 * 发起会话信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "clientId": "接收者ID",
        "audio"   : 是否需要声音（true|false），
        "video"   : 是否需要视频（true|false）
    }
    """,
    flow = {
        "终端->信令服务->终端",
        "终端=>信令服务->终端"
    }
)
public class SessionCallProtocol extends ProtocolSessionAdapter {

    public static final String SIGNAL = "session::call";
    
    public SessionCallProtocol() {
        super("发起会话信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String targetId = MapUtils.get(body, Constant.CLIENT_ID);
        final Client target   = this.clientManager.clients(targetId);
        final Session session = this.sessionManager.call(client, target);
        message.setBody(Map.of(
            Constant.NAME,       target.status().getName(),
            Constant.CLIENT_ID,  target.clientId(),
            Constant.SESSION_ID, session.getId()
        ));
        client.push(message);
        final Message callMessage = message.cloneWithoutBody();
        callMessage.setBody(Map.of(
            Constant.NAME,       client.status().getName(),
            Constant.CLIENT_ID,  client.clientId(),
            Constant.SESSION_ID, session.getId(),
            Constant.AUDIO,      MapUtils.get(body, Constant.AUDIO),
            Constant.VIDEO,      MapUtils.get(body, Constant.VIDEO)
        ));
        target.push(callMessage);
    }

}
