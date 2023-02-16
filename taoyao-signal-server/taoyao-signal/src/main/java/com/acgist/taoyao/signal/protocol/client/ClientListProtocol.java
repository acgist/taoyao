package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端列表信令
 * 
 * @author acgist
 */
@Protocol
public class ClientListProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::list";
	
	public ClientListProtocol() {
		super("终端列表信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		message.setBody(this.clientManager.status());
		client.push(message);
	}
	
}
