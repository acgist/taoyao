package com.acgist.taoyao.signal.protocol.media;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.platform.PlatformScriptEvent;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 重启媒体信令
 * 
 * @author acgist
 */
@Slf4j
public class MediaRebootProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "media::reboot";
	
	@Autowired
	private ScriptProperties scriptProperties;
	
	public MediaRebootProtocol() {
		super("重启媒体信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		log.info("重启媒体：{}", sn);
		// 全员广播
		this.clientSessionManager.broadcast(message);
		// 推送事件
		this.publishEvent(new PlatformScriptEvent(this.scriptProperties.getMediaReboot(), message, session));
	}

}
