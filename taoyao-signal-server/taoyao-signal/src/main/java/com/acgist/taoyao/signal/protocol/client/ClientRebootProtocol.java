package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 重启终端信令
 * 
 * @author acgist
 */
@Protocol
public class ClientRebootProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "client::reboot";
	
	public ClientRebootProtocol() {
		super("重启终端信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
	}
	
}
