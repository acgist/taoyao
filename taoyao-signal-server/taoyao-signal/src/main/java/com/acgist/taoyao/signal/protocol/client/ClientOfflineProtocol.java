package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 终端下线信令
 * 
 * @author acgist
 */
@Protocol
public class ClientOfflineProtocol extends ProtocolAdapter {
	
	public static final String SIGNAL = "client::offline";

	public ClientOfflineProtocol() {
		super("终端下线信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Client client, Message message) {
		// 忽略
	}
	
}
