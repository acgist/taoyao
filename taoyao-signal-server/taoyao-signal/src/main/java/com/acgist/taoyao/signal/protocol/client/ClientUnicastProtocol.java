package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Protocol;
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
@Protocol
public class ClientUnicastProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "client::unicast";
	
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	public ClientUnicastProtocol() {
		super("单播信令", SIGNAL);
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
