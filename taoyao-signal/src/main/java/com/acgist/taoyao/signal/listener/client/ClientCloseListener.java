package com.acgist.taoyao.signal.listener.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.client.ClientCloseEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.protocol.client.ClientOfflineProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端关闭监听
 * 
 * @author acgist
 */
@Slf4j
@EventListener
public class ClientCloseListener extends ApplicationListenerAdapter<ClientCloseEvent> {

	@Autowired
	private ClientOfflineProtocol offlineProtocol;
	
	@Override
	public void onApplicationEvent(ClientCloseEvent event) {
		final ClientSession session = event.getSession();
		if(!session.authorized()) {
			// 没有授权终端
			return;
		}
		final String sn = event.getSn();
		log.info("关闭终端：{}", sn);
		// 广播下线事件
		final Message message = this.offlineProtocol.build(
			Map.of("sn", sn)
		);
		this.clientSessionManager.broadcast(sn, message);
		// TODO：释放连接
		// TODO：释放会议
		// TODO：退出帐号
		// TODO：注意释放：是否考虑没有message（非正常的关闭）不要立即释放
	}

}
