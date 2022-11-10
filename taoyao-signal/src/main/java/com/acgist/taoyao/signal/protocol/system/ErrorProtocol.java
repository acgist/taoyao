package com.acgist.taoyao.signal.protocol.system;

import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;
import com.acgist.taoyao.signal.session.ClientSession;

/**
 * 异常信令协议
 * 
 * @author acgist
 */
@Component
public class ErrorProtocol extends ProtocolAdapter {

	/**
	 * 信令协议标识
	 */
	public static final Integer PID = 9999;
	
	public ErrorProtocol() {
		super(PID);
	}
	
	@Override
	public ApplicationEvent execute(String sn, Message message, ClientSession session) {
		return null;
	}

}
