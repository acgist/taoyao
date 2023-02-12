package com.acgist.taoyao.signal.client.socket;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
public class SocketClient extends ClientAdapter<AsynchronousSocketChannel> {

	public SocketClient(AsynchronousSocketChannel instance) {
		super(instance);
	}

	@Override
	public void push(Message message) {
		try {
			synchronized (this.instance) {
			}
			if(this.instance.isOpen()) {
				final Future<Integer> future = this.instance.write(ByteBuffer.wrap(message.toString().getBytes()));
				future.get();
				// TODO：超时
			} else {
				log.error("会话已经关闭：{}", this.instance);
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("Socket发送消息异常：{}", message, e);
		}
	}
	
}
