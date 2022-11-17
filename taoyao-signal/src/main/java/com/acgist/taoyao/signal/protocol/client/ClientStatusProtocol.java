package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 终端状态信令
 * 
 * @author acgist
 */
@Component
public class ClientStatusProtocol extends ProtocolMapAdapter {

	public static final Integer PID = 2998;
	
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	public ClientStatusProtocol() {
		super(PID, "终端状态信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		String querySn = (String) body.get("sn");
		// 如果没有指定终端标识默认查询自己
		if(StringUtils.isEmpty(querySn)) {
			querySn = sn;
		}
		message.setBody(this.clientSessionManager.status(querySn));
		session.push(message);
	}
	
}
