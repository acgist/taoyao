package com.acgist.taoyao.signal.event.platform;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 执行命令事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class ScriptEvent extends ApplicationEventAdapter {

	private static final long serialVersionUID = 1L;
	
	public ScriptEvent(String sn, Map<?, ?> body, Message message, ClientSession session) {
		super(sn, body, message, session);
	}

}
