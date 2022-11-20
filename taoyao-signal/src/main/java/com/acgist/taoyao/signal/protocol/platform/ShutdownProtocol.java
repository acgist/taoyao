package com.acgist.taoyao.signal.protocol.platform;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭服务信令
 * 
 * @author acgist
 */
@Slf4j
@Component
public class ShutdownProtocol extends ProtocolAdapter {

	public static final Integer PID = 1000;
	
	public ShutdownProtocol() {
		super(PID, "关闭服务信令");
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		// 全员广播
		this.clientSessionManager.broadcast(message);
		// 关闭信令服务
		if(this.context instanceof ConfigurableApplicationContext context) {
			log.info("关闭信令服务：{}", sn);
			if(context.isActive()) {
				context.close();
			}
		} else {
			log.info("关闭服务失败：{}", sn);
		}
	}

}
