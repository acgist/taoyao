package com.acgist.taoyao.signal.client;

import org.apache.commons.lang3.StringUtils;

/**
 * 会话适配器
 * 
 * @author acgist
 */
public abstract class ClientSessionAdapter<T extends AutoCloseable> implements ClientSession {

	/**
	 * 终端标识
	 */
	protected String sn;
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
	protected ClientSessionStatus status;
	
	protected ClientSessionAdapter(T instance) {
		this.time = System.currentTimeMillis();
		this.instance = instance;
		this.authorized = false;
		this.status = new ClientSessionStatus();
	}

	@Override
	public String sn() {
		return this.sn;
	}
	
	@Override
	public ClientSessionStatus status() {
		return this.status;
	}
	
	@Override
	public boolean timeout(long timeout) {
		return !this.authorized && System.currentTimeMillis() - this.time > timeout;
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
	public boolean filterSn(String sn) {
		return StringUtils.equals(sn, this.sn);
	}
	
	@Override
	public boolean filterNoneSn(String sn) {
		return !StringUtils.equals(sn, this.sn);
	}
	
	@Override
	public <I extends AutoCloseable> boolean matchInstance(I instance) {
		return instance == this.instance;
	}
	
	@Override
	public void close() throws Exception {
		this.instance.close();
	}
	
	/**
	 * @return 会话实例
	 */
	public T instance() {
		return this.instance;
	}
	
}
