package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端下线信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
        {
            "clientId": "下线终端标识"
        }
        """,
    flow = "终端-[终端关闭]>信令服务-)终端"
)
public class ClientOfflineProtocol extends ProtocolClientAdapter {
	
	public static final String SIGNAL = "client::offline";

	public ClientOfflineProtocol() {
		super("终端下线信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 忽略
	}
	
}
