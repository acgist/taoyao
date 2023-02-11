package com.acgist.taoyao.signal.mediasoup;

import java.io.Closeable;
import java.util.List;

import com.acgist.taoyao.signal.client.Client;

/**
 * 传输通道
 * 
 * @author acgist
 */
public class Transport implements Closeable {

	/**
	 * 终端
	 */
	private Client client;
	/**
	 * 生产者列表
	 */
	private List<Stream> producers;
	/**
	 * 消费者列表
	 */
	private List<Stream> consumers;
	
	@Override
	public void close() {
		this.producers.forEach(Stream::close);
		this.consumers.forEach(Stream::close);
	}
	
}
