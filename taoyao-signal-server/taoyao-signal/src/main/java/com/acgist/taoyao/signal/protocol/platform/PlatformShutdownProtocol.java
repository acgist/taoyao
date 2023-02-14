package com.acgist.taoyao.signal.protocol.platform;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.platform.PlatformShutdownEvent;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭平台信令
 * 
 * @author acgist
 */
@Slf4j
public class PlatformShutdownProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "platform::shutdown";
	
	public PlatformShutdownProtocol() {
		super("关闭平台信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Client client, Message message) {
		log.info("关闭平台：{}", sn);
		// 全员广播
		this.clientManager.broadcast(message);
		// 推送事件
		this.publishEvent(new PlatformShutdownEvent(message, client));
	}

}
