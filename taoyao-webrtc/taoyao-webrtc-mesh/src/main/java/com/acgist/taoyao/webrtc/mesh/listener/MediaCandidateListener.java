package com.acgist.taoyao.webrtc.mesh.listener;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.media.MediaCandidateEvent;
import com.acgist.taoyao.signal.listener.MediaListenerAdapter;

/**
 * 候选监听
 * 
 * @author acgist
 */
public class MediaCandidateListener extends MediaListenerAdapter<MediaCandidateEvent> {

	@Override
	public void onApplicationEvent(MediaCandidateEvent event) {
		final String sn = event.getSn();
		final List<String> sns = event.getSns();
		if(CollectionUtils.isEmpty(sns)) {
			return;
		}
		final Message message = event.getMessage();
		final Map<String, Object> mergeBody = event.mergeBody();
		mergeBody.put("from", sn);
		sns.forEach(to -> this.clientSessionManager.unicast(to, message));
	}

}
