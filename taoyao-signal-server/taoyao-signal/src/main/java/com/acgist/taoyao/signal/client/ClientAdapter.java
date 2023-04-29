package com.acgist.taoyao.signal.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端适配器
 * 
 * @author acgist
 * 
 * @param <T> 实例泛型
 */
@Slf4j
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
     * 超时时间
     */
    protected final long timeout;
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
	protected final ClientStatus status;
	/**
     * 同步消息
     */
    protected final Map<Long, Message> requestMessage;
	
	protected ClientAdapter(long timeout, T instance) {
		this.time = System.currentTimeMillis();
		this.timeout = timeout;
		this.instance = instance;
		this.authorized = false;
		this.status = new ClientStatus();
		this.requestMessage = new ConcurrentHashMap<>();
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
	public ClientType clientType() {
	    return this.status.getClientType();
	}
	
	@Override
	public ClientStatus status() {
		return this.status;
	}
	
	@Override
    public Message request(Message request) {
        final Header header = request.getHeader();
        final Long id = header.getId();
        this.requestMessage.put(id, request);
        synchronized (request) {
            this.push(request);
            try {
                request.wait(this.timeout);
            } catch (InterruptedException e) {
                log.error("终端等待响应异常：{}", request, e);
            }
        }
        final Message response = this.requestMessage.remove(id);
        if (response == null || request.equals(response)) {
            log.warn("终端没有响应：{}", request);
            throw MessageCodeException.of(MessageCode.CODE_2001, "终端没有响应");
        }
        return response;
    }
	
	@Override
	public boolean response(Long id, Message message) {
        final Message request = this.requestMessage.get(id);
        if (request != null) {
            // 同步处理：重新设置响应消息
            this.requestMessage.put(id, message);
            // 唤醒等待线程
            synchronized (request) {
                request.notifyAll();
            }
            return true;
        } else {
            return false;
        }
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
	public void close() throws Exception {
		this.instance.close();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " - " + this.ip + " - " + this.clientId;
	}
	
}
