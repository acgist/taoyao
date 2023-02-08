package com.acgist.taoyao.signal.mediasoup;

import java.io.Closeable;
import java.util.List;

import com.acgist.taoyao.signal.client.ClientSession;

/**
 * 传输通道
 * 
 * @author acgist
 */
public class Transport implements Closeable {

	/**
	 * 终端
	 */
	private ClientSession clientSession;
	/**
	 * 生产者列表
	 */
	private List<Stream> producerList;
	/**
	 * 消费者列表
	 */
	private List<Stream> consumerList;
	
	@Override
	public void close() {
		this.producerList.forEach(Stream::close);
		this.consumerList.forEach(Stream::close);
	}
	
}
