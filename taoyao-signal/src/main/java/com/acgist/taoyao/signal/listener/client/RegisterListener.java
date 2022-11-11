package com.acgist.taoyao.signal.listener.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.client.RegisterEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.protocol.client.OnlineProtocol;
import com.acgist.taoyao.signal.session.ClientSession;

/**
 * 终端注册监听
 * 
 * @author acgist
 */
@Component
public class RegisterListener extends ApplicationListenerAdapter<RegisterEvent> {

	@Autowired
	private OnlineProtocol onlineProtocol;

	@Async
	@Override
	public void onApplicationEvent(RegisterEvent event) {
		final ClientSession session = event.getSession();
		if (!session.authorized()) {
			return;
		}
		final Message message = this.onlineProtocol.build();
		message.setBody(Map.of("sn", session.sn()));
		this.clientSessionManager.broadcast(session.sn(), message);
		// TODO：ip等等
		// TODO：重新注册上来需要掉线重连
	}

}
