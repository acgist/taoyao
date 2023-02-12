package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 终端广播信令
 * 
 * @author acgist
 */
@Protocol
public class ClientBroadcastProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "client::broadcast";
	
	public ClientBroadcastProtocol() {
		super("终端广播信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Client client, Message message) {
		this.clientManager.broadcast(client, message);
	}

}
