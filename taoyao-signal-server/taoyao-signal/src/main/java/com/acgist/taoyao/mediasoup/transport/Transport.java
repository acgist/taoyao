package com.acgist.taoyao.mediasoup.transport;

import java.util.List;

import com.acgist.taoyao.mediasoup.client.ClientStream;
import com.acgist.taoyao.signal.client.ClientSession;

/**
 * 传输通道
 * 
 * @author acgist
 */
public final class Transport {

	/**
	 * 终端
	 */
	private ClientSession clientSession;
	/**
	 * 生产者列表
	 */
	private List<ClientStream> producerList;
	/**
	 * 消费者列表
	 */
	private List<ClientStream> consumerList;
	
}
