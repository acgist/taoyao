package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端心跳信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "latitude": 纬度,
        "longitude": 经度,
        "humidity": 湿度,
        "temperature": 温度,
        "signal": 信号强度（0~100）,
        "battery": 电池电量（0~100）,
        "alarming": 是否发生告警（true|false）,
        "charging": 是否正在充电（true|false）,
        "recording": 是否正在录像（true|false）,
        "lastHeartbeat": "最后心跳时间",
        "status": {更多状态},
        "config": {更多配置}
    }
    """,
    flow = "终端->信令服务->终端"
)
public class ClientHeartbeatProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::heartbeat";
	
	public ClientHeartbeatProtocol() {
		super("终端心跳信令", SIGNAL);
	}
	
	@Override
	public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
		client.push(message.cloneWithoutBody());
		final ClientStatus status = client.getStatus();
		status.copy(body);
	}
	
}
