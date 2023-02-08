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
public class PlatformScriptEvent extends ApplicationEventAdapter {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 脚本
	 */
	public static final String SCRIPT = "script";
	/**
	 * 结果
	 */
	public static final String RESULT = "result";
	
	public PlatformScriptEvent(String script, Message message, ClientSession session) {
		this(Map.of(SCRIPT, script), message, session);
	}
	
	public PlatformScriptEvent(Map<?, ?> body, Message message, ClientSession session) {
		super(body, message, session);
	}
	
	/**
	 * @return {@link #SCRIPT}
	 */
	public String getScript() {
		return this.get(SCRIPT);
	}

}
