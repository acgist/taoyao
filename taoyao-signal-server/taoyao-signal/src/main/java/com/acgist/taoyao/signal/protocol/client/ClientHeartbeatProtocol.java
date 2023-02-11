package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 终端心跳信令
 * 
 * @author acgist
 */
@Protocol
public class ClientHeartbeatProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "client::heartbeat";
	
	public ClientHeartbeatProtocol() {
		super("终端心跳信令", SIGNAL);
	}
	
	@Override
	public void execute(String sn, Map<?, ?> body, Client client, Message message) {
		// 响应心跳
		client.push(message.cloneWidthoutBody());
		// 设置状态
		final ClientStatus status = client.status();
		status.setSignal((Integer) body.get(Constant.SIGNAL));
		status.setBattery((Integer) body.get(Constant.BATTERY));
		status.setCharging((Boolean) body.get(Constant.CHARGING));
		status.setLastHeartbeat(LocalDateTime.now());
	}
	
}
