package com.acgist.taoyao.webrtc.mesh.listener;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.media.MediaOfferEvent;
import com.acgist.taoyao.signal.listener.MediaListenerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * Offer监听
 * 
 * @author acgist
 */
@Slf4j
public class MediaOfferListener extends MediaListenerAdapter<MediaOfferEvent> {

	@Override
	public void onApplicationEvent(MediaOfferEvent event) {
		final String sn = event.getSn();
		final String to = event.getTo();
		if(sn.equals(to)) {
			log.debug("忽略Offer消息（相同终端）：{}-{}", sn, to);
			return;
		}
		final Message message = event.getMessage();
		final Map<String, Object> mergeBody = event.mergeBody();
		mergeBody.put("from", sn);
		this.clientSessionManager.unicast(to, message);
	}

}
