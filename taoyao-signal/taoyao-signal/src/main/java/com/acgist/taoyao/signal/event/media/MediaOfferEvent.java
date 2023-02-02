package com.acgist.taoyao.signal.event.media;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * Offer事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class MediaOfferEvent extends ApplicationEventAdapter {

	private static final long serialVersionUID = 1L;
	
	public MediaOfferEvent(String sn, Map<?, ?> body, Message message, ClientSession session) {
		super(sn, body, message, session);
	}

	/**
	 * @return 接收终端标识
	 */
	public String getTo() {
		return this.get("to");
	}
	
}
