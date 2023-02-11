package com.acgist.taoyao.signal.protocol;

import java.net.http.WebSocket;

import com.acgist.taoyao.boot.model.Message;

/**
 * 媒体服务信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolMediaAdapter extends ProtocolAdapter {

	protected ProtocolMediaAdapter(String name, String signal) {
		super(name, signal);
	}
	
	/**
	 * 处理媒体服务信令
	 * 
	 * @param message 信令消息
	 * @param webSocket WebSocket
	 */
	public abstract void execute(Message message, WebSocket webSocket);
	
}
