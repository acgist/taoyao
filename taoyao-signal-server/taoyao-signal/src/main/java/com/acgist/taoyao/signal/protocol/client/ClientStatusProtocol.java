package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolMapAdapter;

/**
 * 终端状态信令
 * 
 * @author acgist
 */
@Protocol
public class ClientStatusProtocol extends ProtocolMapAdapter {

	public static final String SIGNAL = "client::status";
	
	public ClientStatusProtocol() {
		super("终端状态信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Map<?, ?> body, Client client, Message message) {
		// 如果没有指定终端标识默认查询自己
		String querySn = (String) body.get(Constant.SN);
		if(StringUtils.isEmpty(querySn)) {
			querySn = sn;
		}
		message.setBody(this.clientManager.status(querySn));
		client.push(message);
	}
	
}
