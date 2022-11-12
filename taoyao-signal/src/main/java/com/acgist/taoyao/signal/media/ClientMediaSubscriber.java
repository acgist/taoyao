package com.acgist.taoyao.signal.media;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.acgist.taoyao.signal.media.stream.ClientMediaStream;

/**
 * 终端媒体订阅者（终端拉流）
 * 
 * @author acgist
 */
public class ClientMediaSubscriber implements ClientMediaHandler {

	/**
	 * 订阅终端媒体流
	 */
	private List<ClientMediaStream> streams = new CopyOnWriteArrayList<>();
	
	/**
	 * 订阅
	 * 
	 * @param id 终端媒体流ID
	 * 
	 * @see #open(String)
	 */
	public void subscribe(String id) {
		this.open(id);
	}
	
	/**
	 * 取消订阅
	 * 
	 * @param id 终端媒体流ID
	 * 
	 * @see #close(String)
	 */
	public void unsubscribe(String id) {
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close(String id) {
		// TODO Auto-generated method stub
		
	}
	
}
