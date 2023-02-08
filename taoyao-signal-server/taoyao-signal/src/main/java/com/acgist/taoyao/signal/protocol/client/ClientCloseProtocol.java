package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端关闭信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
public class ClientCloseProtocol extends ProtocolAdapter {

	public static final String SIGNAL = "client::close";
	
	public ClientCloseProtocol() {
		super("终端关闭信令", SIGNAL);
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		// 关闭不会响应
		try {
			session.close();
		} catch (Exception e) {
			log.error("关闭终端异常", e);
		}
		// 不用发布事件：关闭连接后会发布事件
	}

}
