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
 * 终端状态信令
 * 
 * @author acgist
 */
@Slf4j
@Component
public class StatusProtocol extends ProtocolMapAdapter {

	/**
	 * 信令协议标识
	 */
	public static final Integer PID = 2007;
	
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	public StatusProtocol() {
		super(PID, "终端状态信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		String querySn = (String) body.get("sn");
		// 如果没有指定终端标识默认查询自己
		if(StringUtils.isEmpty(querySn)) {
			querySn = sn;
		}
		final ClientSession clientSession = this.clientSessionManager.session(querySn);
		if(clientSession != null) {
			message.setBody(clientSession.status());
			session.push(message);
		} else {
			log.warn("终端无效：{}", querySn);
		}
	}
	
}
