package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.client.ClientCloseEvent;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端关闭信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
public class ClientCloseProtocol extends ProtocolClientAdapter implements ApplicationListener<ClientCloseEvent> {

	public static final String SIGNAL = "client::close";
	
    @Autowired
    private ClientOfflineProtocol offlineProtocol;
	
	public ClientCloseProtocol() {
		super("终端关闭信令", SIGNAL);
	}
	
	@Override
	public void onApplicationEvent(ClientCloseEvent event) {
	    this.close(event.getClient());
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 响应消息
		client.push(message.cloneWidthoutBody());
		// 不用发布事件：关闭连接后会发布事件
		try {
			client.close();
		} catch (Exception e) {
			log.error("关闭终端异常", e);
		}
	}
	
	/**
	 * 关闭终端
	 * 
	 * @param client 终端
	 */
	public void close(Client client) {
        if(!client.authorized()) {
            // 没有授权终端
            return;
        }
        final String clientId = client.clientId();
        log.info("关闭终端：{}", clientId);
        // 房间释放
        this.roomManager.leave(client);
        // 广播下线事件
        final Message message = this.offlineProtocol.build(
            Map.of(Constant.CLIENT_ID, clientId)
        );
        this.clientManager.broadcast(clientId, message);
        // TODO：释放连接
        // TODO：释放房间
        // TODO：退出帐号
        // TODO：注意释放：是否考虑没有message（非正常的关闭）不要立即释放
	}

}
