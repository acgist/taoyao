package com.acgist.taoyao.signal.event.room;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 进入房间事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class RoomEnterEvent extends ApplicationEventAdapter {
	
	private static final long serialVersionUID = 1L;

	public RoomEnterEvent(String sn, Map<?, ?> body, Message message, ClientSession session) {
		super(sn, body, message, session);
	}

}
