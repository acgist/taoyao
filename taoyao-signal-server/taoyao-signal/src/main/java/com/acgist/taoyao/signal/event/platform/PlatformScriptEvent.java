package com.acgist.taoyao.signal.event.platform;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;
import com.acgist.taoyao.signal.protocol.Constant;

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
	
	public PlatformScriptEvent(String script, Client client, Message message) {
		this(Map.of(Constant.SCRIPT, script), client, message);
	}
	
	public PlatformScriptEvent(Map<?, ?> body, Client client, Message message) {
		super(body, client, message);
	}
	
	/**
	 * @return {@link Constant#SCRIPT}
	 */
	public String getScript() {
		return this.get(Constant.SCRIPT);
	}

}
