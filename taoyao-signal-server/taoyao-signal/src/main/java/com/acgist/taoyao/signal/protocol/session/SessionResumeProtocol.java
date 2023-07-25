package com.acgist.taoyao.signal.protocol.session;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.session.Session;
import com.acgist.taoyao.signal.protocol.ProtocolSessionAdapter;

/**
 * 恢复媒体信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "sessionId": "会话ID",
        "type"     : "媒体类型（audio|voice）"
    }
    """,
    flow = "终端->信令服务->终端"
)
public class SessionResumeProtocol extends ProtocolSessionAdapter {
    
    public static final String SIGNAL = "session::resume";
    
    public SessionResumeProtocol() {
        super("恢复媒体信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Session session, Client client, Message message, Map<String, Object> body) {
        session.pushRemote(clientId, message);
    }

}
