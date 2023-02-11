package com.acgist.taoyao.signal.listener.client;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.event.client.ClientRegisterEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.protocol.client.ClientConfigProtocol;
import com.acgist.taoyao.signal.protocol.client.ClientOnlineProtocol;

/**
 * 终端注册监听
 * 
 * @author acgist
 */
@EventListener
public class ClientRegisterListener extends ApplicationListenerAdapter<ClientRegisterEvent> {

	@Autowired
	private ClientConfigProtocol configProtocol;
	@Autowired
	private ClientOnlineProtocol onlineProtocol;
	
	@Async
	@Override
	public void onApplicationEvent(ClientRegisterEvent event) {
		final Client client = event.getClient();
		if (!client.authorized()) {
			return;
		}
		final String sn = event.getSn();
		// 下发配置
		client.push(this.configProtocol.build());
		// 终端状态
		final ClientStatus status = client.status();
		status.setSn(sn);
		status.setIp(StringUtils.defaultString(client.ip(), event.getIp()));
		status.setSignal(event.getSignal());
		status.setBattery(event.getBattery());
		status.setCharging(event.getCharging());
		// 上线事件
		this.clientManager.broadcast(
			sn,
			this.onlineProtocol.build(status)
		);
	}

}
