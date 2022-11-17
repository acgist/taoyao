package com.acgist.taoyao.signal.protocol.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 广播信令
 * 
 * @author acgist
 */
@Component
public class ClientBroadcastProtocol extends ProtocolAdapter {

	public static final Integer PID = 2007;
	
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	public ClientBroadcastProtocol() {
		super(PID, "广播信令");
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		this.clientSessionManager.broadcast(sn, message);
	}

}
