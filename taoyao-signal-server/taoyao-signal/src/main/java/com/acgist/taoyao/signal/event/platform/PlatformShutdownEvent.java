package com.acgist.taoyao.signal.event.platform;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.event.ClientEventAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 关闭平台事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class PlatformShutdownEvent extends ClientEventAdapter {

	private static final long serialVersionUID = 1L;
	
	public PlatformShutdownEvent(Message message, Client client) {
		super(message, client);
	}

}
