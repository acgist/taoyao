package com.acgist.taoyao.signal.protocol.system;

import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;
import com.acgist.taoyao.signal.session.ClientSession;

/**
 * 心跳信令协议
 * 
 * @author acgist
 */
@Component
public class HeartbeatProtocol extends ProtocolAdapter {

	/**
	 * 信令协议标识
	 */
	public static final Integer PID = 1000;
	
	public HeartbeatProtocol() {
		super(PID);
	}
	
	@Override
	public ApplicationEvent execute(String sn, Message message, ClientSession session) {
		session.push(message);
		return null;
	}

}
