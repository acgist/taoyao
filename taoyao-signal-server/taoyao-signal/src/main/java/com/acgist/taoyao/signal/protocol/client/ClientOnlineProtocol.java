package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端上线信令
 * 
 * @author acgist
 */
@Protocol
public class ClientOnlineProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::online";
	
	public ClientOnlineProtocol() {
		super("终端上线信令", SIGNAL);
	}
	
	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 忽略
	}

}
