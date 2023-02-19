package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端上线信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
        {
            "ip": "终端IP",
            "mediaId": "媒体服务标识",
            "clientId": "终端标识",
            "signal": 信号强度（0~100）,
            "battery": 电池电量（0~100）,
            "charging": 是否正在充电（true|false）,
            "lastHeartbeat": "最后心跳时间"
        }
        """,
    flow = "终端-[终端注册]>信令服务-)终端"
)
public class ClientOnlineProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::online";
	
	public ClientOnlineProtocol() {
		super("终端上线信令", SIGNAL);
	}
	
	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 忽略
	}

}
