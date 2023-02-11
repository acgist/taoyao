package com.acgist.taoyao.signal.client.socket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientAdapter;

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
public class SocketClient extends ClientAdapter<Socket> {

	/**
	 * 输出
	 */
	private OutputStream outputStream;
	
	public SocketClient(Socket instance) {
		super(instance);
		try {
			this.outputStream = instance.getOutputStream();
		} catch (IOException e) {
			log.error("Socket终端输出异常：{}", instance, e);
		}
	}

	@Override
	public void push(Message message) {
		try {
			if(this.instance.isClosed()) {
				log.error("会话已经关闭：{}", this.instance);
			} else {
				this.outputStream.write(message.toString().getBytes());
			}
		} catch (IOException e) {
			log.error("Socket发送消息异常：{}", message, e);
		}
	}
	
}
