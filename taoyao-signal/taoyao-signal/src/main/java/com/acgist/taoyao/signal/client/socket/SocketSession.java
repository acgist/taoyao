package com.acgist.taoyao.signal.client.socket;

import java.io.IOException;
import java.net.Socket;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSessionAdapter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Socket会话
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class SocketSession extends ClientSessionAdapter<Socket> {

	public SocketSession(Socket instance) {
		super(instance);
	}

	@Override
	public void push(Message message) {
		try {
			this.instance.getOutputStream().write(message.toString().getBytes());
		} catch (IOException e) {
			log.error("Socket发送消息异常：{}", message, e);
		}
	}

}
