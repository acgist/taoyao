package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionStatus;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 心跳信令
 * 
 * @author acgist
 */
@Component
public class ClientHeartbeatProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 2005;
	
	public ClientHeartbeatProtocol() {
		super(PID, "心跳信令");
	}
	
	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		// 响应心跳
		session.push(message.cloneWidthoutBody());
		// 设置状态
		final ClientSessionStatus status = session.status();
		status.setSignal((Integer) body.get(ClientSessionStatus.SIGNAL));
		status.setBattery((Integer) body.get(ClientSessionStatus.BATTERY));
		status.setLastHeartbeat(LocalDateTime.now());
	}

}
