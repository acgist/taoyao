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
 * 媒体交换信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    memo = """
        媒体交换协商：offer/answer/candidate
        安卓需要注意：
            1. 交换类型大小写
            2. candidate内容默认名称sdp
    """,
    body = """
    {
        "sdp"      : "sdp"
        "type"     : "offer|answer",
        "sessionId": "会话ID"
    }
    {
        "type"     : "candidate",
        "sessionId": "会话ID",
        "candidate": {
            "sdpMid"       : "sdpMid",
            "candidate"    : "candidate",
            "sdpMLineIndex": sdpMLineIndex 
        }
    }
    """,
    flow = "终端->信令服务->终端"
)
public class SessionExchangeProtocol extends ProtocolSessionAdapter {
    
    public static final String SIGNAL = "session::exchange";
    
    public SessionExchangeProtocol() {
        super("媒体交换信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Session session, Client client, Message message, Map<String, Object> body) {
        session.pushRemote(clientId, message);
    }

}
