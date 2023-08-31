package com.acgist.taoyao.signal.protocol.session;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
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
    body = {
        """
        {
            "clientId": "目标ID",
            "audio"   : 是否需要声音（true|false），
            "video"   : 是否需要视频（true|false）
        }
        """,
        """
        {
            "name"     : "终端名称",
            "clientId" : "终端ID",
            "sessionId": "会话ID"
        }
        """
    },
    flow = "终端=>信令服务->终端"
)
public class SessionCallProtocol extends ProtocolSessionAdapter {

    public static final String SIGNAL = "session::call";
    
    public SessionCallProtocol() {
        super("发起会话信令", SIGNAL);
    }
    
    @Override
    public boolean authenticate(Client client, Message message) {
        return true;
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String targetId = MapUtils.get(body, Constant.CLIENT_ID);
        final Client target   = this.clientManager.getClients(targetId);
        if(target == null) {
            throw MessageCodeException.of(MessageCode.CODE_3404, "邀请对象无效");
        }
        final Session session = this.sessionManager.call(client, target);
        message.setBody(Map.of(
            Constant.NAME,       target.getName(),
            Constant.CLIENT_ID,  target.getClientId(),
            Constant.SESSION_ID, session.getId()
        ));
        client.push(message);
        final Message callMessage = message.cloneWithoutBody();
        callMessage.setBody(Map.of(
            Constant.NAME,       client.getName(),
            Constant.CLIENT_ID,  client.getClientId(),
            Constant.SESSION_ID, session.getId(),
            Constant.AUDIO,      MapUtils.get(body, Constant.AUDIO, true),
            Constant.VIDEO,      MapUtils.get(body, Constant.VIDEO, true)
        ));
        target.push(callMessage);
    }

}
