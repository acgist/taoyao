package com.acgist.taoyao.signal.party.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.signal.client.Client;

/**
 * P2P会话管理器
 * 
 * @author acgist
 */
@Manager
public class SessionManager {

    private final IdService idService;
    private final Map<String, Session> sessions;
    
    public SessionManager(IdService idService) {
        this.idService = idService;
        this.sessions = new ConcurrentHashMap<>();
    }
    
    /**
     * @param source 发起者
     * @param target 接收者
     * 
     * @return 会话
     */
    public Session call(Client source, Client target) {
        final Session session = new Session(this.idService.buildUuid(), source, target);
        this.sessions.put(session.getId(), session);
        return session;
    }
    
    /**
     * @param sessionId 会话ID
     * 
     * @return 会话
     */
    public Session get(String sessionId) {
        return this.sessions.get(sessionId);
    }
    
}
