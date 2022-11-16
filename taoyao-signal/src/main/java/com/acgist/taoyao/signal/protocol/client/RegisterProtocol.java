package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.client.RegisterEvent;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 注册信令
 * 
 * @author acgist
 */
@Component
public class RegisterProtocol extends ProtocolMapAdapter {

	/**
	 * 信令协议标识
	 */
	public static final Integer PID = 2000;
	
	@Autowired
	private SecurityProperties securityProperties;
	
	public RegisterProtocol() {
		super(PID, "注册信令");
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Message message, ClientSession session) {
		final String username = (String) body.get("username");
		final String password = (String) body.get("password");
		// 如果需要终端鉴权在此实现
		if(
			Boolean.FALSE.equals(this.securityProperties.getEnabled()) ||
			StringUtils.equals(this.securityProperties.getUsername(), username) &&
			StringUtils.equals(this.securityProperties.getPassword(), password)
		) {
			session.authorize(sn);
			message.setCode(MessageCode.CODE_0000);
		} else {
			message.setCode(MessageCode.CODE_3401);
		}
		// 推送消息
		session.push(message.cloneWidthoutBody());
		// 发送事件
		this.publishEvent(new RegisterEvent(body, message, session));
	}

}
