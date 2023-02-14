package com.acgist.taoyao.signal.client.socket;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.acgist.taoyao.boot.property.SocketProperties;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令接收处理器
 * 
 * @author acgist
 */
@Slf4j
public final class SocketSignalAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

	private ClientManager clientManager;
	private ProtocolManager protocolManager;
	private SocketProperties socketProperties;
	private PlatformErrorProtocol platformErrorProtocol;
	
	public SocketSignalAcceptHandler(
		ClientManager clientManager,
		ProtocolManager protocolManager,
		SocketProperties socketProperties,
		PlatformErrorProtocol platformErrorProtocol
	) {
		this.clientManager = clientManager;
		this.protocolManager = protocolManager;
		this.socketProperties = socketProperties;
		this.platformErrorProtocol = platformErrorProtocol;
	}

	@Override
	public void completed(AsynchronousSocketChannel channel, AsynchronousServerSocketChannel server) {
		try {
			channel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
			this.clientManager.open(new SocketClient(this.socketProperties.getTimeout(), channel));
			final SocketSignalMessageHandler messageHandler = new SocketSignalMessageHandler(
				this.socketProperties.getBufferSize(),
				this.clientManager,
				this.protocolManager,
				channel,
				this.platformErrorProtocol
			);
			messageHandler.loopMessage();
			log.debug("Socket信令连接成功：{}", channel);
		} catch (IOException e) {
			log.error("Socket信令连接异常", e);
		} finally {
			server.accept(server, this);
		}
	}
	
	@Override
	public void failed(Throwable throwable, AsynchronousServerSocketChannel server) {
		log.error("Socket信令连接异常：{}", server, throwable);
	}
	
}