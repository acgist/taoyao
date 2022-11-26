package com.acgist.taoyao.signal.event.media;

import java.util.List;
import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 候选事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaCandidateEvent extends ApplicationEventAdapter {

	private static final long serialVersionUID = 1L;
	
	public MediaCandidateEvent(String sn, Map<?, ?> body, Message message, ClientSession session) {
		super(sn, body, message, session);
	}

	/**
	 * @return 终端列表
	 */
	public List<String> getSns() {
		return this.get("sns");
	}
	
}
