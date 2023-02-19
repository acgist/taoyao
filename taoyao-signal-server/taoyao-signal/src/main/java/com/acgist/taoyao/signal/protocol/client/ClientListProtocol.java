package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端列表信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
        [
            {
                "clientId": "终端标识",
                "ip": "终端IP",
                "signal": 信号强度（0~100）,
                "battery": 电池电量（0~100）,
                "charging": 是否正在充电（true|false）,
                "mediaId": "媒体服务标识",
                "lastHeartbeat": "最后心跳时间"
            },
            ...
        ]
        """,
    flow = "终端->信令服务->终端"
)
public class ClientListProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::list";
	
	public ClientListProtocol() {
		super("终端列表信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		message.setBody(this.clientManager.status());
		client.push(message);
	}
	
}
