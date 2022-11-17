package com.acgist.taoyao.signal.protocol.client;

import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭信令
 * 
 * @author acgist
 */
@Slf4j
@Component
public class CloseProtocol extends ProtocolAdapter {

	public static final Integer PID = 2001;
	
	public CloseProtocol() {
		super(PID, "关闭信令");
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
