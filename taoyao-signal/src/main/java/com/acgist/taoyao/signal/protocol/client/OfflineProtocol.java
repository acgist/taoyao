package com.acgist.taoyao.signal.protocol.client;

import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 终端下线信令
 * 
 * @author acgist
 */
@Component
public class OfflineProtocol extends ProtocolAdapter {
	
	public static final Integer PID = 2003;

	public OfflineProtocol() {
		super(PID, "终端下线信令");
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		// 忽略
	}
	
}
