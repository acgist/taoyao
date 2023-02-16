package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端心跳信令
 * 
 * @author acgist
 */
@Protocol
public class ClientHeartbeatProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::heartbeat";
	
	public ClientHeartbeatProtocol() {
		super("终端心跳信令", SIGNAL);
	}
	
	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 响应心跳
		client.push(message.cloneWidthoutBody());
		// 设置状态
		final ClientStatus status = client.status();
		status.setSignal(this.get(body, Constant.SIGNAL));
		status.setBattery(this.get(body, Constant.BATTERY));
		status.setCharging(this.get(body, Constant.CHARGING));
		status.setLastHeartbeat(LocalDateTime.now());
	}
	
}
