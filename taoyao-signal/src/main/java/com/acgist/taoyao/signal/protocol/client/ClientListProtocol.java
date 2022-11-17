package com.acgist.taoyao.signal.protocol.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 终端列表信令
 * 
 * @author acgist
 */
@Component
public class ClientListProtocol extends ProtocolAdapter {

	public static final Integer PID = 2999;
	
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	public ClientListProtocol() {
		super(PID, "终端列表信令");
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		message.setBody(this.clientSessionManager.status());
		session.push(message);
	}
	
}
