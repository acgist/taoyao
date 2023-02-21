package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端单播信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
        {
            "to": "接收终端标识",
            // 主体信息
        }
        """,
    flow = "终端->信令服务->终端"
)
public class ClientUnicastProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::unicast";
	
	public ClientUnicastProtocol() {
		super("终端单播信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		final String to = (String) body.remove(Constant.TO);
		if(StringUtils.isNotEmpty(to)) {
			this.clientManager.unicast(to, message);
		} else {
			log.warn("终端单播消息没有接收终端标识：{}", to);
		}
	}
	
}
