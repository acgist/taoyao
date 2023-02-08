package com.acgist.taoyao.signal.listener.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionStatus;
import com.acgist.taoyao.signal.event.client.ClientRegisterEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.mediasoup.MediasoupClient;
import com.acgist.taoyao.signal.mediasoup.MediasoupClientManager;
import com.acgist.taoyao.signal.protocol.client.ClientConfigProtocol;
import com.acgist.taoyao.signal.protocol.client.ClientOnlineProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端注册监听
 * 
 * TODO：如果已经在房间中，自动推流。
 * 
 * @author acgist
 */
@Slf4j
@EventListener
public class ClientRegisterListener extends ApplicationListenerAdapter<ClientRegisterEvent> {

	@Autowired
	private ClientConfigProtocol configProtocol;
	@Autowired
	private ClientOnlineProtocol onlineProtocol;
	@Autowired
	private MediasoupClientManager mediasoupClientManager;
	
	@Async
	@Override
	public void onApplicationEvent(ClientRegisterEvent event) {
		final ClientSession session = event.getSession();
		if (!session.authorized()) {
			return;
		}
		final String sn = event.getSn();
		// 下发配置
		session.push(this.configProtocol.build());
		// 修改终端状态
		final ClientSessionStatus status = session.status();
		status.setSn(sn);
		status.setIp(event.getIp());
		status.setMac(event.getMac());
		status.setSignal(event.getSignal());
		status.setBattery(event.getBattery());
		status.setCharging(event.getCharging());
		status.setMediasoup(event.getMediasoup());
		// 设置终端媒体
		final MediasoupClient mediasoupClient = this.mediasoupClientManager.mediasoupClient(event.getMediasoup());
		if(mediasoupClient == null) {
			log.warn("Mediasoup服务无效：{}", event.getMediasoup());
		} else {
			session.mediasoupClient(mediasoupClient);
		}
		// 广播上线事件
		this.clientSessionManager.broadcast(
			sn,
			this.onlineProtocol.build(status)
		);
	}

}
