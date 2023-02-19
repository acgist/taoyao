package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端广播信令
 * 
 * @author acgist
 */
@Protocol
@Description(flow = "终端->信令服务-)终端")
public class ClientBroadcastProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::broadcast";
	
	public ClientBroadcastProtocol() {
		super("终端广播信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		this.clientManager.broadcast(client, message);
	}

}
