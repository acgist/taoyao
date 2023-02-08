package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionStatus;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 心跳信令
 * 
 * @author acgist
 */
@Protocol
public class ClientHeartbeatProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "client::heartbeat";
	
	public ClientHeartbeatProtocol() {
		super("心跳信令", SIGNAL);
	}
	
	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		// 响应心跳
		session.push(message.cloneWidthoutBody());
		// 设置状态
		final ClientSessionStatus status = session.status();
		status.setSignal((Integer) body.get(ClientSessionStatus.SIGNAL));
		status.setBattery((Integer) body.get(ClientSessionStatus.BATTERY));
		status.setCharging((Boolean) body.get(ClientSessionStatus.CHARGING));
		status.setLastHeartbeat(LocalDateTime.now());
	}

}
