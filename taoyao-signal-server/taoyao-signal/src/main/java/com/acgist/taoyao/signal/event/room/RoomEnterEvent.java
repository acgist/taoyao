package com.acgist.taoyao.signal.event.room;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;
import com.acgist.taoyao.signal.protocol.Constant;

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

	public RoomEnterEvent(Map<?, ?> body, Client client, Message message) {
		super(body, client, message);
	}
	
	/**
	 * @return {@link Constant#ID}
	 */
	public Long getId() {
		return this.getLong(Constant.ID);
	}
	
	/**
	 * @return {@link Constant#PASSWORD}
	 */
	public String getPassword() {
		return this.get(Constant.PASSWORD);
	}

}
