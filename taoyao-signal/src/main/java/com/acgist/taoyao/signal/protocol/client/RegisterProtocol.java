package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.event.client.RegisterEvent;
import com.acgist.taoyao.signal.protocol.ProtocolBodyMapAdapter;
import com.acgist.taoyao.signal.session.ClientSession;

/**
 * 注册信令协议
 * 
 * @author acgist
 */
@Component
public class RegisterProtocol extends ProtocolBodyMapAdapter {

	/**
	 * 信令协议标识
	 */
	public static final Integer PID = 2000;
	
	@Autowired
	private SecurityProperties securityProperties;
	
	public RegisterProtocol() {
		super(PID);
	}

	@Override
	public ApplicationEvent execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		final String username = (String) body.get("username");
		final String password = (String) body.get("password");
		if(
			StringUtils.equals(this.securityProperties.getUsername(), username) &&
			StringUtils.equals(this.securityProperties.getPassword(), password)
		) {
			session.authorize(sn);
			message.setCode(MessageCode.CODE_0000);
		} else {
			message.setCode(MessageCode.CODE_3401);
		}
		session.push(message);
		return new RegisterEvent(session);
	}

}
