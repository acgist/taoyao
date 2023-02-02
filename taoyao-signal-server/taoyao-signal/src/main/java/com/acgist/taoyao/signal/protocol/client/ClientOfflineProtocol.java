package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 终端下线信令
 * 
 * @author acgist
 */
@Protocol
public class ClientOfflineProtocol extends ProtocolAdapter {
	
	public static final Integer PID = 2003;

	public ClientOfflineProtocol() {
		super(PID, "终端下线信令");
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		// 忽略
	}
	
}
