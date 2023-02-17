package com.acgist.taoyao.signal.client;

import com.acgist.taoyao.signal.media.MediaClient;

/**
 * 终端适配器
 * 
 * @author acgist
 * 
 * @param <T> 实例泛型
 */
public abstract class ClientAdapter<T extends AutoCloseable> implements Client {

	/**
	 * IP
	 */
	protected String ip;
	/**
	 * 进入时间
	 */
	protected final long time;
	/**
	 * 终端标识
	 */
	protected String clientId;
	/**
	 * 终端实例
	 */
	protected final T instance;
	/**
	 * 是否授权
	 */
	protected boolean authorized;
	/**
	 * 终端状态
	 */
	protected ClientStatus status;
	/**
	 * 媒体服务终端
	 */
	protected MediaClient mediaClient;
	
	protected ClientAdapter(T instance) {
		this.time = System.currentTimeMillis();
		this.instance = instance;
		this.authorized = false;
		this.status = new ClientStatus();
	}
	
	@Override
	public String ip() {
		return this.ip;
	}
	
	@Override
	public String clientId() {
	    return this.clientId;
	}
	
	@Override
	public ClientStatus status() {
		return this.status;
	}
	
	@Override
	public boolean timeout(long timeout) {
		return System.currentTimeMillis() - this.time > timeout;
	}
	
	@Override
	public T instance() {
		return this.instance;
	}
	
	@Override
	public void authorize(String clientId) {
		this.clientId = clientId;
		this.authorized = true;
	}
	
	@Override
	public boolean authorized() {
		return this.authorized;
	}
	
	@Override
	public MediaClient mediaClient() {
		return this.mediaClient;
	}
	
	@Override
	public void mediaClient(MediaClient mediaClient) {
		this.mediaClient = mediaClient;
		this.status.setMediaId(mediaClient.mediaId());
	}
	
	@Override
	public void close() throws Exception {
		this.instance.close();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " - " + this.ip + " - " + this.clientId;
	}
	
}
