package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.client.ClientRegisterEvent;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;
import com.acgist.taoyao.signal.service.SecurityService;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端注册信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
public class ClientRegisterProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "client::register";
	
	@Autowired
	private SecurityService securityService;
	
	public ClientRegisterProtocol() {
		super("终端注册信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Client client, Message message) {
		final String username = (String) body.get(Constant.USERNAME);
		final String password = (String) body.get(Constant.PASSWORD);
		// 如果需要终端鉴权在此实现
		if(this.securityService.authenticate(username, password)) {
			log.info("终端注册：{}", sn);
			client.authorize(sn);
			message.setCode(MessageCode.CODE_0000);
		} else {
			message.setCode(MessageCode.CODE_3401);
		}
		// 推送消息
		client.push(message.cloneWidthoutBody());
		// 发送事件
		this.publishEvent(new ClientRegisterEvent(body, client, message));
	}
	
}
