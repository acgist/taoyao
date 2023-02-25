package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端告警信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
    {
        "message": "告警描述",
        "datetime": "告警时间（yyyyMMddHHmmss）"
    }
    """,
    flow = "终端->信令服务"
)
public class ClientAlarmProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "client::alarm";
    
    public ClientAlarmProtocol() {
        super("终端告警信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        log.warn(
            """
            终端发生告警：{}
            {}
            {}
            """,
            clientId,
            MapUtils.get(body, Constant.MESSAGE),
            MapUtils.get(body, Constant.DATETIME)
        );
    }
    
}
