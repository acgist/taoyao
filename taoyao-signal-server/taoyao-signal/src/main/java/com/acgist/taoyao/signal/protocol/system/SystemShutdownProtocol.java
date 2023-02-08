package com.acgist.taoyao.signal.protocol.system;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.platform.PlatformScriptEvent;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭系统信令
 * 
 * @author acgist
 */
@Slf4j
public class SystemShutdownProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "system::shutdown";
	
	@Autowired
	private ScriptProperties scriptProperties;
	
	public SystemShutdownProtocol() {
		super("关闭系统信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		log.info("关闭系统：{}", sn);
		this.clientSessionManager.broadcast(message);
		this.publishEvent(new PlatformScriptEvent(this.scriptProperties.getSystemShutdown(), message, session));
	}

}
