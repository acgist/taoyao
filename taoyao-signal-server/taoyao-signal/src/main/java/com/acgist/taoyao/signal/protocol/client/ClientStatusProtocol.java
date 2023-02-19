package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.Constant;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端状态信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "clientId": "终端标识"
        }
        """,
        """
        {
            "clientId": "终端标识",
            "ip": "终端IP",
            "signal": 信号强度（0~100）,
            "battery": 电池电量（0~100）,
            "charging": 是否正在充电（true|false）,
            "mediaId": "媒体服务标识",
            "lastHeartbeat": "最后心跳时间"
        }
        """
    },
    flow = "终端->信令服务->终端"
)
public class ClientStatusProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::status";
	
	public ClientStatusProtocol() {
		super("终端状态信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 如果没有指定终端标识默认查询自己
		message.setBody(this.clientManager.status(this.get(body, Constant.CLIENT_ID, clientId)));
		client.push(message);
	}
	
}
