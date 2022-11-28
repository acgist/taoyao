package com.acgist.taoyao.webrtc.mesh.listener;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.media.MediaSubscribeEvent;
import com.acgist.taoyao.signal.listener.MediaListenerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 订阅监听
 * 
 * @author acgist
 */
@Slf4j
public class MediaSubscribeListener extends MediaListenerAdapter<MediaSubscribeEvent> {

	@Override
	public void onApplicationEvent(MediaSubscribeEvent event) {
		final String sn = event.getSn();
		final String to = event.getTo();
		if(sn.equals(to)) {
			log.debug("忽略订阅消息（相同终端）：{}-{}", sn, to);
			return;
		}
		final Message message = event.getMessage();
		final Map<String, Object> mergeBody = event.mergeBody();
		mergeBody.put("from", sn);
		this.clientSessionManager.unicast(to, message);
	}

}
