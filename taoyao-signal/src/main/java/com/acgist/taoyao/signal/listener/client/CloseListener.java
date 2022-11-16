package com.acgist.taoyao.signal.listener.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.client.CloseEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;
import com.acgist.taoyao.signal.protocol.client.OfflineProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭监听
 * 
 * @author acgist
 */
@Slf4j
@Component
public class CloseListener extends ApplicationListenerAdapter<CloseEvent> {

	@Autowired
	private OfflineProtocol offlineProtocol;
	
	@Override
	public void onApplicationEvent(CloseEvent event) {
		final ClientSession session = event.getSession();
		final String sn = session.sn();
		if(StringUtils.isEmpty(sn)) {
			// 没有授权终端
			return;
		}
		log.info("关闭终端：{}", sn);
		// 广播下线事件
		final Message message = this.offlineProtocol.build(
			Map.of("sn", sn)
		);
		this.clientSessionManager.broadcast(sn, message);
		// TODO：释放连接
		// TODO：释放房间
		// TODO：退出房间
		// TODO：退出帐号
		// TODO：注意释放：是否考虑没有message（非正常的关闭）不要立即释放
	}

}
