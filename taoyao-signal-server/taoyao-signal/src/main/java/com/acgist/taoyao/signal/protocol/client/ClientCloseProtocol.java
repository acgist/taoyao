package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.client.ClientCloseEvent;
import com.acgist.taoyao.signal.event.client.ClientOfflineEvent;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭终端信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    memo = "同时释放所有资源，所以如果终端意外掉线重连，需要终端实现音视频重连逻辑。",
    flow = {
        "终端->信令服务->终端",
        "终端->信令服务-[终端下线])终端"
    }
)
public class ClientCloseProtocol extends ProtocolClientAdapter implements ApplicationListener<ClientCloseEvent> {

	public static final String SIGNAL = "client::close";
	
	public ClientCloseProtocol() {
		super("关闭终端信令", SIGNAL);
	}
	
	@Async
	@Override
	public void onApplicationEvent(ClientCloseEvent event) {
	    this.close(event.getClient());
	}

	@Override
	public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
		client.push(message.cloneWithoutBody());
		try {
		    // 关闭连接后会发布事件
			client.close();
		} catch (Exception e) {
			log.error("关闭终端异常：{}", clientId, e);
		}
	}
	
	/**
	 * 关闭终端
	 * 
	 * @param client 终端
	 */
	private void close(Client client) {
        if(client == null || !client.authorized()) {
            // 没有授权终端
            return;
        }
        final String clientId = client.clientId();
        log.info("关闭终端：{}", clientId);
        // 释放房间终端
        this.roomManager.leave(client);
        // 终端下线事件
        this.publishEvent(new ClientOfflineEvent(client));
	}

}
