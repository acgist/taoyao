package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务重启信令
 * 
 * @author acgist
 */
@Slf4j
public class MediaRebootProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "media::reboot";
	
	@Autowired
	private ScriptProperties scriptProperties;
	
	public MediaRebootProtocol() {
		super("重启媒体服务信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		log.info("重启媒体服务：{}", clientId);
		this.clientManager.broadcast(message);
		ScriptUtils.execute(this.scriptProperties.getMediaReboot());
	}

}
