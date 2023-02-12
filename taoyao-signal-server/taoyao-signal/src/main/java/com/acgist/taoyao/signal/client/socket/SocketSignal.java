package com.acgist.taoyao.signal.client.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.property.SocketProperties;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令
 * 
 * @author acgist
 */
@Slf4j
public class SocketSignal {

	private int index = 0;
	private ClientManager clientManager;
	private AsynchronousChannelGroup group;
	private ProtocolManager protocolManager;
	private SocketProperties socketProperties;
	private AsynchronousServerSocketChannel channel;
	
	public SocketSignal(
		ClientManager clientManager,
		ProtocolManager protocolManager,
		SocketProperties socketProperties
	) {
		this.clientManager = clientManager;
		this.protocolManager = protocolManager;
		this.socketProperties = socketProperties;
		try {
			final ExecutorService executor = new ThreadPoolExecutor(
				this.socketProperties.getThreadMin(),
				this.socketProperties.getThreadMax(),
				this.socketProperties.getKeepAliveTime(),
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(this.socketProperties.getQueueSize()),
				newThreadFactory()
			);
			this.group = AsynchronousChannelGroup.withThreadPool(executor);
		} catch (IOException e) {
			throw MessageCodeException.of(e, "创建Socket信令失败");
		}
	}
	
	/**
	 * @return 线程池工厂
	 */
	private ThreadFactory newThreadFactory() {
		return (runnable) -> {
			final Thread thread = new Thread(runnable);
			// 线程名称
			synchronized(this) {
				if(++this.index > this.socketProperties.getThreadMax()) {
					this.index = 0;
				}
				thread.setName(this.socketProperties.getThreadNamePrefix() + this.index);
			}
			// 守护线程
			thread.setDaemon(true);
			return thread;
		};
	}

	/**
	 * 开启监听
	 * 
	 * @return 是否成功
	 */
	public boolean listen() {
		boolean success = true;
		try {
			this.channel = AsynchronousServerSocketChannel.open(this.group);
			this.channel.bind(new InetSocketAddress(this.socketProperties.getHost(), this.socketProperties.getPort()));
			this.channel.accept(this.channel, new SocketAcceptHandler(this.clientManager, this.protocolManager));
		} catch (IOException e) {
			log.error("启动Socket信令服务异常", e);
			success = false;
		} finally {
			if(success) {
				log.info("启动Socket信令服务：{}-{}", this.socketProperties.getHost(), this.socketProperties.getPort());
			} else {
				this.shutdown();
			}
		}
		return success;
	}
	
	/**
	 * 关闭Socket信令服务
	 */
	public void shutdown() {
		log.debug("关闭Socket信令服务");
		CloseableUtils.close(this.channel);
		this.group.shutdown();
	}
	
}
