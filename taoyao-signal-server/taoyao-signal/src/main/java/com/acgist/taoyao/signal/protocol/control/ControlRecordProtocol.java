package com.acgist.taoyao.signal.protocol.control;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 录像信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    memo = "状态通过心跳回传",
    flow = {
        "信令服务->终端",
        "终端->信令服务->终端"
    }
)
public class ControlRecordProtocol extends ProtocolControlAdapter {

    public static final String SIGNAL = "control::record";
    
    public ControlRecordProtocol() {
        super("录像信令", SIGNAL);
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
