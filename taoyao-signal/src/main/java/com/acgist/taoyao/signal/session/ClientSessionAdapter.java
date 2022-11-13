package com.acgist.taoyao.signal.session;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.signal.media.ClientMediaPublisher;
import com.acgist.taoyao.signal.media.ClientMediaSubscriber;

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
	/**
	 * 终端媒体发布者
	 */
	protected ClientMediaPublisher publisher;
	/**
	 * 终端媒体订阅者
	 */
	protected ClientMediaSubscriber subscriber;
	
	protected ClientSessionAdapter(T instance) {
		this.time = System.currentTimeMillis();
		this.instance = instance;
		this.authorized = false;
		this.status = new ClientSessionStatus();
		this.publisher = new ClientMediaPublisher();
		this.subscriber = new ClientMediaSubscriber();
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
	public ClientMediaPublisher publisher() {
		return this.publisher;
	}
	
	@Override
	public ClientMediaSubscriber subscriber() {
		return this.subscriber;
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
	public boolean matchSn(String sn) {
		return StringUtils.equals(sn, this.sn);
	}
	
	@Override
	public boolean matchNoneSn(String sn) {
		return !StringUtils.equals(sn, this.sn);
	}
	
	@Override
	public <I extends AutoCloseable> boolean matchInstance(I instance) {
		return instance == this.instance;
	}
	
	@Override
	public void close() throws Exception {
		try {
			this.instance.close();
		} finally {
			// TODO：退出房间
			// TODO：退出帐号
		}
	}
	
	/**
	 * @return 会话实例
	 */
	public T instance() {
		return this.instance;
	}
	
}
