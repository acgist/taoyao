package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 终端唤醒信令
 * 
 * 注意：不能自己唤醒自己
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "to": "目标终端ID"
    }
    """,
    flow = {
        "信令服务=>终端",
        "终端=>信令服务->终端"
    }
)
public class ControlWakeupProtocol extends ProtocolControlAdapter implements IControlWakeupProtocol {

    private static final String SIGNAL = "control::wakeup";
    
    public ControlWakeupProtocol() {
        super("终端唤醒信令", SIGNAL);
    }
    
    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        client.push(targetClient.request(message));
    }

    @Override
    public Message execute(String clientId) {
        return this.request(clientId, this.build(Map.of()));
    }
    
}
