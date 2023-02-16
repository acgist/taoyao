package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 重启终端信令
 * 
 * @author acgist
 */
@Protocol
public class ClientRebootProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::reboot";
	
	public ClientRebootProtocol() {
		super("重启终端信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 忽略
	}
	
}
