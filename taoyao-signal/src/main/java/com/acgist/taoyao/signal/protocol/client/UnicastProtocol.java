package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 单播信令
 * 
 * @author acgist
 */
@Slf4j
@Component
public class UnicastProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 2008;
	
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	public UnicastProtocol() {
		super(PID, "单播信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		final String to = (String) body.remove("to");
		if(StringUtils.isNotEmpty(to)) {
			this.clientSessionManager.unicast(to, message);
		} else {
			log.warn("单播消息没有接收终端标识：{}", to);
		}
	}
	
}
