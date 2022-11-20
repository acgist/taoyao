package com.acgist.taoyao.signal.media.stream;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端媒体流发布者（终端推流）
 * 
 * 通过处理器发送给订阅者
 * 
 * @author acgist
 */
@Slf4j
public class ClientMediaPublisher implements ClientMediaHandler {

	/**
	 * 发布终端媒体流
	 */
	private Map<String, ClientMediaStream> streams = new ConcurrentHashMap<>();

	/**
	 * 发布
	 * 
	 * @param id 终端媒体流ID
	 * 
	 * @see #open(String)
	 */
	public void publish(String id) {
		this.open(id);
	}
	
	/**
	 * 取消发布
	 * 
	 * @param id 终端媒体流ID
	 * 
	 * @see #close(String)
	 */
	public void unpublish(String id) {
		this.close(id);
	}

	@Override
	public void open(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume(String id) {
		final ClientMediaStream stream = this.streams.get(id);
		if(stream != null) {
			try {
				stream.resume();
			} catch (IOException e) {
				log.error("终端媒体流恢复异常：{}", id, e);
			}
		}
	}

	@Override
	public void close(String id) {
		final ClientMediaStream stream = this.streams.get(id);
		try {
			stream.close();
		} catch (IOException e) {
			log.error("终端媒体流关闭异常：{}", id, e);
		}
	}
	
}
