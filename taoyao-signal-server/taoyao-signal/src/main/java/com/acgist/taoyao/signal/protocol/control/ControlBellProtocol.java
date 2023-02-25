package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 响铃信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    flow = {
        "信令服务->终端",
        "终端->信令服务->终端"
    }
)
public class ControlBellProtocol extends ProtocolControlAdapter {

    private static final String SIGNAL = "control::bell";
    
    public ControlBellProtocol() {
        super("响铃信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        targetClient.push(message);
    }

    /**
     * @param clientId 终端标识
     */
    public void execute(String clientId) {
        this.clientManager.unicast(clientId, this.build());
    }
    
}
