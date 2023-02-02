package com.acgist.taoyao.media.listener;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.media.MediaCandidateEvent;
import com.acgist.taoyao.signal.listener.MediaListenerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 候选监听
 * 
 * @author acgist
 */
@Slf4j
@EventListener
public class MediaCandidateListener extends MediaListenerAdapter<MediaCandidateEvent> {

	@Override
	public void onApplicationEvent(MediaCandidateEvent event) {
		final String sn = event.getSn();
		final String to = event.getTo();
		if(sn.equals(to)) {
			log.debug("忽略候选消息（相同终端）：{}-{}", sn, to);
			return;
		}
		final Message message = event.getMessage();
		final Map<String, Object> mergeBody = event.mergeBody();
		mergeBody.put("from", sn);
		this.clientSessionManager.unicast(to, message);
	}

}
