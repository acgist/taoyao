package com.acgist.taoyao.signal.protocol.media;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.platform.PlatformScriptEvent;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务重启信令
 * 
 * @author acgist
 */
@Slf4j
public class MediaRebootProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "media::reboot";
	
	@Autowired
	private ScriptProperties scriptProperties;
	
	public MediaRebootProtocol() {
		super("重启媒体服务信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Client client, Message message) {
		log.info("重启媒体服务：{}", sn);
		// 全员广播
		this.clientManager.broadcast(message);
		// 推送事件
		this.publishEvent(new PlatformScriptEvent(this.scriptProperties.getMediaReboot(), client, message));
	}

}
