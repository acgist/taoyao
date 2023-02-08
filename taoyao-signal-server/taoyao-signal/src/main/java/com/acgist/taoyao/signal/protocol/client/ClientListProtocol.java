package com.acgist.taoyao.signal.protocol.client;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 终端列表信令
 * 
 * @author acgist
 */
@Protocol
public class ClientListProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "client::list";
	
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	public ClientListProtocol() {
		super("终端列表信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		message.setBody(this.clientSessionManager.status());
		session.push(message);
	}
	
}
