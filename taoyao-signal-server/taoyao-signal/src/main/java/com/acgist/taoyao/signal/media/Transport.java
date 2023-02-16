package com.acgist.taoyao.signal.media;

import java.io.Closeable;

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
	
	@Override
	public void close() {
	}
	
}
