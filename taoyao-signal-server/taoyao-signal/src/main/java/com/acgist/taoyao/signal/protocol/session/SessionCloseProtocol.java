package com.acgist.taoyao.signal.protocol.session;

import java.util.Map;

import org.springframework.context.ApplicationListener;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.session.SessionCloseEvent;
import com.acgist.taoyao.signal.party.session.Session;
import com.acgist.taoyao.signal.protocol.ProtocolSessionAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭媒体信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
    {
        "sessionId": "会话ID"
    }
    """,
    flow = "终端->信令服务->终端"
)
public class SessionCloseProtocol extends ProtocolSessionAdapter implements ApplicationListener<SessionCloseEvent> {
    
    public static final String SIGNAL = "session::close";
    
    public SessionCloseProtocol() {
        super("关闭媒体信令", SIGNAL);
    }
    
    @Override
    public void onApplicationEvent(SessionCloseEvent event) {
        final Session session = event.getSession();
        session.push(this.build(Map.of(
            Constant.SESSION_ID,
            event.getSessionId()
        )));
        this.sessionManager.remove(session.getId());
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Session session, Client client, Message message, Map<String, Object> body) {
        log.info("关闭会话：{}", session.getId());
        session.close();
    }
    
}
