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
 * 关闭媒体信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
    }
    """,
    flow = "终端->信令服务+)终端"
)
public class SessionCloseProtocol extends ProtocolSessionAdapter {
    
    public static final String SIGNAL = "session::close";
    
    public SessionCloseProtocol() {
        super("关闭媒体信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Session session, Client client, Message message, Map<String, Object> body) {
        session.push(message);
        session.close();
    }
    
}
