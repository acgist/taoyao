package com.acgist.taoyao.signal.event.platform;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 关闭平台事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class PlatformShutdownEvent extends ApplicationEventAdapter {

	private static final long serialVersionUID = 1L;
	
	public PlatformShutdownEvent(Client client, Message message) {
		super(client, message);
	}

}
