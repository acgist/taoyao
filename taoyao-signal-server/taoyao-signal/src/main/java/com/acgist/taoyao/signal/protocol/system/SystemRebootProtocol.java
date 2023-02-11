package com.acgist.taoyao.signal.protocol.system;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.platform.PlatformScriptEvent;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 重启系统信令
 * 
 * @author acgist
 */
@Slf4j
public class SystemRebootProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "system::reboot";
	
	@Autowired
	private ScriptProperties scriptProperties;
	
	public SystemRebootProtocol() {
		super("重启系统信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Client client, Message message) {
		log.info("重启系统：{}", sn);
		this.clientManager.broadcast(message);
		this.publishEvent(new PlatformScriptEvent(this.scriptProperties.getSystemReboot(), client, message));
	}

}
