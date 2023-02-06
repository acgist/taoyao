package com.acgist.taoyao.signal.protocol;

import java.net.http.WebSocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.mediasoup.MediasoupClient;
import com.acgist.taoyao.signal.client.ClientSession;

/**
 * Mediasoup信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolMediasoupAdapter extends ProtocolAdapter {

	@Lazy
	@Autowired
	protected MediasoupClient mediasoupClient;
	
	protected ProtocolMediasoupAdapter(Integer pid, String name) {
		super(pid, name);
	}
	
	@Override
	public void execute(String sn, Message message, ClientSession session) {
	}
	
	/**
	 * 处理Mediasoup信令
	 * 
	 * @param message 信令消息
	 * @param webSocket WebSocket
	 */
	public abstract void execute(Message message, WebSocket webSocket);
	
}
