package com.acgist.taoyao.signal.client.socket;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令消息
 * 
 * @author acgist
 */
@Slf4j
public final class SocketMessageHandler implements CompletionHandler<Integer, ByteBuffer> {

	private ClientManager clientManager;
	private ProtocolManager protocolManager;
	private AsynchronousSocketChannel channel;
	
	public SocketMessageHandler(ClientManager clientManager, ProtocolManager protocolManager) {
		this.clientManager = clientManager;
		this.protocolManager = protocolManager;
	}

	public void handle(AsynchronousSocketChannel channel) {
		this.channel = channel;
		this.waitMessage();
	}

	/**
	 * 消息轮询
	 */
	private void waitMessage() {
		if(this.channel.isOpen()) {
			final ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
			this.channel.read(buffer, buffer, this);
		} else {
			log.debug("Socket信令消息退出消息轮询");
		}
	}
	
	private void close() {
		CloseableUtils.close(this.channel);
	}
	
	@Override
	public void completed(Integer result, ByteBuffer buffer) {
		if (result == null) {
			this.close();
		} else if(result == -1) {
			// 服务端关闭
			this.close();
		} else if(result == 0) {
			// 消息空轮询
			log.debug("Socket信令消息接收失败（长度）：{}", result);
		} else {
			final byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			this.protocolManager.execute(new String(bytes), this.channel);
		}
		this.waitMessage();
	}
	
	@Override
	public void failed(Throwable throwable, ByteBuffer buffer) {
		log.error("Socket信令消息处理异常：{}", this.channel, throwable);
		this.close();
	}

	
}