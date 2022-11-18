package com.acgist.taoyao.signal.listener.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionStatus;
import com.acgist.taoyao.signal.event.client.ClientRegisterEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.protocol.client.ClientConfigProtocol;
import com.acgist.taoyao.signal.protocol.client.ClientOnlineProtocol;

/**
 * 终端注册监听
 * 
 * @author acgist
 */
@Component
public class ClientRegisterListener extends ApplicationListenerAdapter<ClientRegisterEvent> {

	@Autowired
	private ClientConfigProtocol configProtocol;
	@Autowired
	private ClientOnlineProtocol onlineProtocol;

	@Async
	@Override
	public void onApplicationEvent(ClientRegisterEvent event) {
		final ClientSession session = event.getSession();
		if (!session.authorized()) {
			return;
		}
		// 下发配置
		session.push(this.configProtocol.build());
		// 广播上线事件
		this.clientSessionManager.broadcast(
			session.sn(),
			this.onlineProtocol.build(Map.of("sn", session.sn()))
		);
		// 修改终端状态
		final Map<?, ?> body = event.getBody();
		final ClientSessionStatus status = session.status();
		status.setSn(session.sn());
		status.setIp((String) body.get("ip"));
		status.setMac((String) body.get("mac"));
		status.setSignal((Integer) body.get("signal"));
		status.setBattery((Integer) body.get("battery"));
	}

}
