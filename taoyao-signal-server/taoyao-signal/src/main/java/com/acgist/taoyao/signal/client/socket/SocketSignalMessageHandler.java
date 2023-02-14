package com.acgist.taoyao.signal.client.socket;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令消息处理器
 * 
 * @author acgist
 */
@Slf4j
public final class SocketSignalMessageHandler implements CompletionHandler<Integer, ByteBuffer> {

	private ClientManager clientManager;
	private ProtocolManager protocolManager;
	private AsynchronousSocketChannel channel;
	private PlatformErrorProtocol platformErrorProtocol;
	
	/**
	 * 换行符号
	 */
	private String line;
	/**
	 * 换行符号长度
	 */
	private int lineLength;
	/**
	 * 缓冲大小
	 */
	private int bufferSize;
	/**
	 * 消息缓存
	 */
	private StringBuilder builder;
	
	public SocketSignalMessageHandler(
		int bufferSize,
		ClientManager clientManager,
		ProtocolManager protocolManager,
		AsynchronousSocketChannel channel,
		PlatformErrorProtocol platformErrorProtocol
	) {
		this.line = Constant.LINE;
		this.lineLength = this.line.length();
		this.builder = new StringBuilder();
		this.channel = channel;
		this.bufferSize = bufferSize;
		this.clientManager = clientManager;
		this.protocolManager = protocolManager;
		this.platformErrorProtocol = platformErrorProtocol;
	}

	/**
	 * 消息轮询
	 */
	public void loopMessage() {
		if(this.channel.isOpen()) {
			final ByteBuffer buffer = ByteBuffer.allocateDirect(this.bufferSize);
			this.channel.read(buffer, buffer, this);
		} else {
			log.debug("Socket信令退出消息轮询");
		}
	}

	/**
	 * 关闭通道
	 */
	private void close() {
		CloseableUtils.close(this.channel);
	}
	
	@Override
	public void completed(Integer result, ByteBuffer buffer) {
		if (result == null) {
			this.close();
		} else if(result < 0) {
			// 服务端关闭
			this.close();
		} else if(result == 0) {
			// 消息空轮询
			log.debug("Socket信令接收消息失败（长度）：{}", result);
		} else {
			buffer.flip();
			final byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			this.builder.append(new String(bytes));
			int index = 0;
			while((index = this.builder.indexOf(this.line)) >= 0) {
				final String message = this.builder.substring(0, index);
				this.builder.delete(0, index + this.lineLength);
				log.debug("Socket信令消息：{}-{}", this.channel, message);
				try {
					this.protocolManager.execute(message.strip(), this.channel);
				} catch (Exception e) {
					log.error("处理Socket信令消息异常：{}", message, e);
					this.clientManager.send(this.channel, this.platformErrorProtocol.build(e));
				}
			}
		}
		this.loopMessage();
	}
	
	@Override
	public void failed(Throwable throwable, ByteBuffer buffer) {
		log.error("Socket信令异常：{}", this.channel, throwable);
		this.close();
	}
	
}