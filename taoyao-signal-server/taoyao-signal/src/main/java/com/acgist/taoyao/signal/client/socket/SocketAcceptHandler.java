package com.acgist.taoyao.signal.client.socket;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令接收
 * 
 * @author acgist
 */
@Slf4j
public final class SocketAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

	private ClientManager clientManager;
	private ProtocolManager protocolManager;
	
	public SocketAcceptHandler(ClientManager clientManager, ProtocolManager protocolManager) {
		this.clientManager = clientManager;
		this.protocolManager = protocolManager;
	}

	@Override
	public void completed(AsynchronousSocketChannel channel, AsynchronousServerSocketChannel server) {
		log.debug("Socket信令连接成功：{}", channel);
		this.clientManager.open(new SocketClient(channel));
		final SocketMessageHandler socketMessageHandler = new SocketMessageHandler(this.clientManager, this.protocolManager);
		socketMessageHandler.handle(channel);
		server.accept(server, this);
	}
	
	@Override
	public void failed(Throwable throwable, AsynchronousServerSocketChannel server) {
		log.error("Socket信令连接异常：{}", server, throwable);
	}
	
}