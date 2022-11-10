package com.acgist.taoyao.signal.session;

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
	
	protected ClientSessionAdapter(T instance) {
		this.time = System.currentTimeMillis();
		this.instance = instance;
		this.authorized = false;
	}

	@Override
	public String sn() {
		return this.sn;
	}
	
	@Override
	public boolean timeout(long timeout) {
		return !(this.authorized && System.currentTimeMillis() - this.time <= timeout);
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
