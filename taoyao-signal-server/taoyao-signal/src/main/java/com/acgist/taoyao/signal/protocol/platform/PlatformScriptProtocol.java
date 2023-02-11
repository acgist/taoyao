package com.acgist.taoyao.signal.protocol.platform;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.platform.PlatformScriptEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 执行命令信令
 * 
 * @author acgist
 */
public class PlatformScriptProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "platform::script";
	
	public PlatformScriptProtocol() {
		super("执行命令信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Client client, Message message) {
		this.publishEvent(new PlatformScriptEvent(body, client, message));
	}
	
}
