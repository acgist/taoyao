package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.signal.event.ClientOfflineEvent;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端下线信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "clientId": "下线终端ID"
    }
    """,
    flow = "终端-[终端关闭]>信令服务-)终端"
)
public class ClientOfflineProtocol extends ProtocolClientAdapter implements ApplicationListener<ClientOfflineEvent> {
	
	public static final String SIGNAL = "client::offline";

	public ClientOfflineProtocol() {
		super("终端下线信令", SIGNAL);
	}

	@Async
    @Override
    public void onApplicationEvent(ClientOfflineEvent event) {
        final String clientId = event.getClientId();
        this.clientManager.broadcast(clientId, this.build(
            Map.of(Constant.CLIENT_ID, clientId)
        ));
    }

}
