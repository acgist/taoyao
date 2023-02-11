package com.acgist.taoyao.signal.client;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.mediasoup.MediasoupClient;

/**
 * 会话适配器
 * 
 * @author acgist
 */
public abstract class ClientAdapter<T extends AutoCloseable> implements Client {

	/**
	 * 终端标识
	 */
	protected String sn;
	/**
	 * IP
	 */
	protected String ip;
	/**
	 * 进入时间
	 */
	protected final long time;
	/**
	 * 会话实例
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
	protected MediasoupClient mediasoupClient;
	
	protected ClientAdapter(T instance) {
		this.time = System.currentTimeMillis();
		this.instance = instance;
		this.authorized = false;
		this.status = new ClientStatus();
	}

	@Override
	public String sn() {
		return this.sn;
	}
	
	@Override
	public String ip() {
		return this.ip;
	}
	
	@Override
	public ClientStatus status() {
		return this.status;
	}
	
	@Override
	public void push(String sn, Message message) {
		final Header header = message.getHeader();
		if(header != null) {
			header.setSn(sn);
		}
		this.push(message);
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
	public void authorize(String sn) {
		this.sn = sn;
		this.authorized = true;
	}
	
	@Override
	public boolean authorized() {
		return this.authorized;
	}
	
	@Override
	public MediasoupClient mediasoupClient() {
		return this.mediasoupClient;
	}
	
	@Override
	public void mediasoupClient(MediasoupClient mediasoupClient) {
		this.mediasoupClient = mediasoupClient;
		this.status.setMediasoup(mediasoupClient.name());
	}
	
	@Override
	public void close() throws Exception {
		this.instance.close();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " - " + this.sn;
	}
	
}
