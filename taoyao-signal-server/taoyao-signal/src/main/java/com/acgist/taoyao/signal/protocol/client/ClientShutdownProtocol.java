package com.acgist.taoyao.signal.protocol.client;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 关闭终端信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    flow = "信令服务->终端"
)
public class ClientShutdownProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "client::shutdown";
    
    public ClientShutdownProtocol() {
        super("关闭终端信令", SIGNAL);
    }
    
    /**
     * @param clientId 终端ID
     */
    public void execute(String clientId) {
        this.clientManager.unicast(clientId, this.build());
    }

}
