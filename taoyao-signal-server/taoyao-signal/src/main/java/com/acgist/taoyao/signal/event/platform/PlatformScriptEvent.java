package com.acgist.taoyao.signal.event.platform;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ClientEventAdapter;
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
public class PlatformScriptEvent extends ClientEventAdapter {

	private static final long serialVersionUID = 1L;
	
	public PlatformScriptEvent(String script, Message message, Client client) {
		this(Map.of(Constant.SCRIPT, script), message, client);
	}
	
	public PlatformScriptEvent(Map<?, ?> body, Message message, Client client) {
		super(body, message, client);
	}
	
	/**
	 * @return {@link Constant#SCRIPT}
	 */
	public String getScript() {
		return this.get(Constant.SCRIPT);
	}

}
