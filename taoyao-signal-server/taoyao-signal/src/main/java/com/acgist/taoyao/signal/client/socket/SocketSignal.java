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

import com.acgist.taoyao.boot.property.SocketProperties;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令
 * 
 * @author acgist
 */
@Slf4j
public class SocketSignal {
	
	private ClientManager clientManager;
	private ProtocolManager protocolManager;
	private SocketProperties socketProperties;
	private PlatformErrorProtocol platformErrorProtocol;

	/**
	 * 线程序号
	 */
	private int index = 0;
	/**
	 * 通道线程池
	 */
	private AsynchronousChannelGroup group;
	/**
	 * 服务端通道
	 */
	private AsynchronousServerSocketChannel channel;
	
	public SocketSignal(
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
	
	/**
	 * 初始化服务端
	 */
	public void init() {
		boolean success = true;
		try {
			final ExecutorService executor = new ThreadPoolExecutor(
				this.socketProperties.getThreadMin(),
				this.socketProperties.getThreadMax(),
				this.socketProperties.getKeepAliveTime(),
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(this.socketProperties.getQueueSize()),
				this.newThreadFactory()
			);
			this.group = AsynchronousChannelGroup.withThreadPool(executor);
			this.channel = AsynchronousServerSocketChannel.open(this.group);
			this.channel.bind(new InetSocketAddress(this.socketProperties.getHost(), this.socketProperties.getPort()));
			this.channel.accept(this.channel, new SocketSignalAcceptHandler(
				this.clientManager,
				this.protocolManager,
				this.socketProperties,
				this.platformErrorProtocol
			));
		} catch (IOException e) {
			log.error("启动Socket信令服务异常", e);
			success = false;
		} finally {
			if(success) {
				log.info("启动Socket信令服务：{}-{}", this.socketProperties.getHost(), this.socketProperties.getPort());
			} else {
				this.destroy();
			}
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
	
	@PreDestroy
	public void destroy() {
		log.debug("关闭Socket信令服务");
		CloseableUtils.close(this.channel);
		this.group.shutdown();
	}
	
}
