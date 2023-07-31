package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端列表信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    memo = "没有选择终端类型时返回所有类型终端状态列表",
    body = {
        """
        {
            "clientType": "终端类型（可选）"
        }
        """,
        """
        [
            {
                "ip"             : "终端IP",
                "name"           : "终端名称",
                "clientId"       : "终端ID",
                "clientType"     : "终端类型",
                "latitude"       : 纬度,
                "longitude"      : 经度,
                "humidity"       : 湿度,
                "temperature"    : 温度,
                "signal"         : 信号强度（0~100）,
                "battery"        : 电池电量（0~100）,
                "alarming"       : 是否发生告警（true|false）,
                "charging"       : 是否正在充电（true|false）,
                "clientRecording": 是否正在录像（true|false）,
                "serverRecording": 是否正在录像（true|false）,
                "lastHeartbeat"  : "最后心跳时间",
                "status"         : {更多状态},
                "config"         : {更多配置}
            },
            ...
        ]
        """
    },
    flow = "终端=>信令服务"
)
public class ClientListProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "client::list";
    
    public ClientListProtocol() {
        super("终端列表信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String queryClientType = MapUtils.get(body, Constant.CLIENT_TYPE);
        if(StringUtils.isEmpty(queryClientType)) {
            message.setBody(this.clientManager.getStatus());
        } else {
            message.setBody(this.clientManager.getStatus(ClientType.of(queryClientType)));
        }
        client.push(message);
    }
    
}
