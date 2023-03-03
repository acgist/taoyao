package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端唤醒信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    flow = "信令服务->终端"
)
public class ClientWakeupProtocol extends ProtocolClientAdapter {

    private static final String SIGNAL = "client::wakeup";
    
    public ClientWakeupProtocol() {
        super("终端唤醒信令", SIGNAL);
    }
    
    /**
     * @param clientId 终端ID
     */
    public void execute(String clientId) {
        this.clientManager.unicast(clientId, this.build());
    }

}
