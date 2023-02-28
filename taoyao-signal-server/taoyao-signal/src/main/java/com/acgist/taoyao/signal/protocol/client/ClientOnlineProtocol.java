package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
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
            "name": "终端名称",
            "clientId": "终端标识",
            "clientType": "终端类型",
            "latitude": 纬度,
            "longitude": 经度,
            "humidity": 湿度,
            "temperature": 温度,
            "signal": 信号强度（0~100）,
            "battery": 电池电量（0~100）,
            "charging": 是否正在充电（true|false）,
            "recording": 是否正在录像（true|false）,
            "status": {更多状态},
            "config": {更多配置}
        }
        """,
    flow = "终端-[终端注册]>信令服务-)终端"
)
public class ClientOnlineProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::online";
	
	public ClientOnlineProtocol() {
		super("终端上线信令", SIGNAL);
	}
	
}
