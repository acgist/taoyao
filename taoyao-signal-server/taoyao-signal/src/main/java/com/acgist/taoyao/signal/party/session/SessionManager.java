package com.acgist.taoyao.signal.party.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.signal.client.Client;

import lombok.extern.slf4j.Slf4j;

/**
 * 视频会话管理器
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class SessionManager {

    private final IdService idService;
    
    /**
     * 会话列表
     */
    private final Map<String, Session> sessions;
    
    public SessionManager(IdService idService) {
        this.idService = idService;
        this.sessions  = new ConcurrentHashMap<>();
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
        log.info("创建视频会话：{} - {} - {}", session.getId(), session.getSourceClientId(), session.getTargetClientId());
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
    
    /**
     * @param sessionId 会话ID
     * 
     * @return 会话
     */
    public Session remove(String sessionId) {
        final Session session = this.sessions.remove(sessionId);
        if(session != null) {
            log.info("移除视频会话：{} - {} - {}", sessionId, session.getSourceClientId(), session.getTargetClientId());
        }
        return session;
    }
    
    /**
     * 关闭所有资源
     * 
     * @param client 终端
     */
    public void close(Client client) {
        this.sessions.values().stream()
        .filter(v -> v.hasClient(client))
        .forEach(Session::close);
    }
    
}
