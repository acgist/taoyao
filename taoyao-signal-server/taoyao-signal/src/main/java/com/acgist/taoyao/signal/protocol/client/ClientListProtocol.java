package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 终端列表信令
 * 
 * @author acgist
 */
@Protocol
public class ClientListProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "client::list";
	
	public ClientListProtocol() {
		super("终端列表信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Client client, Message message) {
		message.setBody(this.clientManager.status());
		client.push(message);
	}
	
}
