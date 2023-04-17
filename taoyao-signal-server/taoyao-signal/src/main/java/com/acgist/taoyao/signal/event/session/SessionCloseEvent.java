package com.acgist.taoyao.signal.event.session;

import com.acgist.taoyao.signal.event.SessionEventAdapter;
import com.acgist.taoyao.signal.party.session.Session;

/**
 * 关闭视频会话事件
 * 
 * @author acgist
 */
public class SessionCloseEvent extends SessionEventAdapter {

    private static final long serialVersionUID = 1L;
    
    public SessionCloseEvent(Session session) {
        super(session);
    }

}
