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
 * 终端广播信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    memo = "没有指定终端类型时广播所有类型终端",
    body = {
        """
        {
            "clientType": "终端类型（可选）"
            ...
        }
        """
    },
    flow = "终端->信令服务-)终端"
)
public class ClientBroadcastProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "client::broadcast";
    
    public ClientBroadcastProtocol() {
        super("终端广播信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final String queryClientType = MapUtils.get(body, Constant.CLIENT_TYPE);
        if(StringUtils.isEmpty(queryClientType)) {
            this.clientManager.broadcast(client, message);
        } else {
            this.clientManager.broadcast(client, message, ClientType.of(queryClientType));
        }
    }

}
