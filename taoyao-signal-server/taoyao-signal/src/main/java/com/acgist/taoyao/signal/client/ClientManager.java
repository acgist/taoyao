package com.acgist.taoyao.signal.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.TaoyaoProperties;
import com.acgist.taoyao.signal.event.client.ClientCloseEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 会话管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class ClientManager {
	
	@Autowired
	private TaoyaoProperties taoyaoProperties;
	@Autowired
	private ApplicationContext applicationContext;
	
	/**
	 * 会话列表
	 */
	private List<Client> clients = new CopyOnWriteArrayList<>();

	@Scheduled(cron = "${taoyao.scheduled.client:0 * * * * ?}")
	public void scheduled() {
		this.closeTimeout();
	}
	
	/**
	 * @param client 终端
	 */
	public void open(Client client) {
		this.clients.add(client);
	}
	
	/**
	 * 单播消息
	 * 
	 * @param to 接收终端
	 * @param message 消息
	 */
	public void unicast(String to, Message message) {
		this.clients().stream()
		.filter(v -> StringUtils.equals(to, v.sn()))
		.forEach(v -> v.push(v.sn(), message));
	}
	
	/**
	 * 广播消息
	 * 
	 * @param message 消息
	 */
	public void broadcast(Message message) {
		this.clients().forEach(v -> v.push(v.sn(), message));
	}
	
	/**
	 * 广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(String from, Message message) {
		this.clients().stream()
		.filter(v -> !StringUtils.equals(from, v.sn()))
		.forEach(v -> v.push(v.sn(), message));
	}
	
	/**
	 * @param sn 终端标识
	 * 
	 * @return 终端会话
	 */
	public Client client(String sn) {
		return this.clients().stream()
			.filter(v -> StringUtils.equals(sn, v.sn()))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * @param instance 终端示例
	 * 
	 * @return 终端
	 */
	public Client client(AutoCloseable instance) {
		return this.clients.stream()
			.filter(v -> v.instance() == instance)
			.findFirst()
			.orElse(null);
	}

	/**
	 * @param sn 终端标识
	 * 
	 * @return 终端状态
	 */
	public ClientStatus status(String sn) {
		final Client client = this.client(sn);
		return client == null ? null : client.status();
	}
	
	/**
	 * @return 所有终端会话
	 */
	public List<Client> clients() {
		return this.clients.stream()
			.filter(Client::authorized)
			.toList();
	}

	/**
	 * @return 所有终端状态
	 */
	public List<ClientStatus> status() {
		return this.clients().stream()
			.map(Client::status)
			.toList();
	}
	
	/**
	 * 关闭会话
	 * 
	 * @param instance 会话实例
	 */
	public void close(AutoCloseable instance) {
		final Client client = this.client(instance);
		// TODO：如果出现异常可以提前移除
		try {
			if(client != null) {
				client.close();
			} else {
				instance.close();
			}
		} catch (Exception e) {
			log.error("关闭会话异常", e);
		} finally {
			if(client != null) {
				// 移除管理
				this.clients.remove(client);
				// 关闭事件
				this.applicationContext.publishEvent(new ClientCloseEvent(client, null));
			}
		}
	}
	
	/**
	 * 定时关闭超时会话
	 */
	private void closeTimeout() {
		log.debug("定时关闭超时会话");
		this.clients.stream()
		.filter(v -> !v.authorized())
		.filter(v -> v.timeout(this.taoyaoProperties.getTimeout()))
		.forEach(v -> {
			log.debug("关闭超时会话：{}", v);
			this.close(v);
		});
	}

}
