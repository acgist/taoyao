package com.acgist.taoyao.signal.event.platform;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

/**
 * 执行命令事件
 * 
 * @author acgist
 */
public class ScriptEvent extends ApplicationEventAdapter {

	private static final long serialVersionUID = 1L;
	
	public ScriptEvent(Map<?, ?> body, Message message, ClientSession session) {
		super(body, message, session);
	}

}
