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

/**
 * 终端单播信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "to": "接收终端ID",
            ...
        }
        """,
        """
        {
            ...
        }
        """
    },
    flow = "终端->信令服务->终端"
)
public class ClientUnicastProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::unicast";
	
	public ClientUnicastProtocol() {
		super("终端单播信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
	    final String to = MapUtils.remove(body, Constant.TO);
		this.clientManager.unicast(to, message);
	}
	
}
