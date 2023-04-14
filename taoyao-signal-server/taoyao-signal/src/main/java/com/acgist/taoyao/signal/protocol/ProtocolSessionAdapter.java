package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.session.Session;
import com.acgist.taoyao.signal.party.session.SessionManager;

/**
 * 会话信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolSessionAdapter extends ProtocolClientAdapter {

    @Autowired
    protected SessionManager sessionManager;
    
	protected ProtocolSessionAdapter(String name, String signal) {
		super(name, signal);
	}
	
	@Override
	public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String sessionId = MapUtils.get(body, Constant.SESSION_ID);
        final Session session = this.sessionManager.get(sessionId);
        if(session == null) {
            throw MessageCodeException.of("无效会话：" + sessionId);
        }
        this.execute(clientId, clientType, session, client, message, body);
	}
	
	/**
	 * 处理终端会话信令
	 * 
	 * @param clientId    终端标识
	 * @param clientType  终端类型
	 * @param session     会话
	 * @param client      终端
	 * @param message     消息
	 * @param body        消息主体
	 */
	public void execute(String clientId, ClientType clientType, Session session, Client client, Message message, Map<String, Object> body) {
	}
	
}
