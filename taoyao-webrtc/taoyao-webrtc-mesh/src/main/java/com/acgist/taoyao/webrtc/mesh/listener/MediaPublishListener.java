package com.acgist.taoyao.webrtc.mesh.listener;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.media.MediaPublishEvent;
import com.acgist.taoyao.signal.listener.MediaListenerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 发布监听
 * 
 * @author acgist
 */
@Slf4j
public class MediaPublishListener extends MediaListenerAdapter<MediaPublishEvent> {

	@Override
	public void onApplicationEvent(MediaPublishEvent event) {
		final String sn = event.getSn();
		final String to = event.getTo();
		if(sn.equals(to)) {
			log.debug("忽略发布消息（相同终端）：{}-{}", sn, to);
			return;
		}
		final Message message = event.getMessage();
		final Map<String, Object> mergeBody = event.mergeBody();
		mergeBody.put("from", sn);
		this.clientSessionManager.unicast(to, message);
	}

}
