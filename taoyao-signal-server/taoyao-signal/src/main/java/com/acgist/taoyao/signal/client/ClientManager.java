package com.acgist.taoyao.signal.client;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.TaoyaoProperties;
import com.acgist.taoyao.signal.event.client.ClientCloseEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端管理器
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
	 * 终端列表
	 */
	private List<Client> clients = new CopyOnWriteArrayList<>();

	@Scheduled(cron = "${taoyao.scheduled.client:0 * * * * ?}")
	public void scheduled() {
		this.closeTimeout();
	}
	
	/**
	 * 终端打开加入管理
	 * 
	 * @param client 终端
	 */
	public void open(Client client) {
		this.clients.add(client);
	}
	
	/**
	 * 授权终端单播消息
	 * 
	 * @param to 接收终端
	 * @param message 消息
	 */
	public void unicast(String to, Message message) {
		this.clients().stream()
		.filter(v -> Objects.equals(to, v.clientId()))
		.forEach(v -> v.push(message));
	}
	
	/**
	 * 授权终端单播消息
	 * 
	 * @param to 接收终端
	 * @param message 消息
	 */
	public void unicast(Client to, Message message) {
		this.clients().stream()
		.filter(v -> v.instance() == to)
		.forEach(v -> v.push(message));
	}
	
	/**
	 * 授权终端广播消息
	 * 
	 * @param message 消息
	 */
	public void broadcast(Message message) {
		this.clients().forEach(v -> v.push(message));
	}
	
	/**
	 * 授权终端广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(String from, Message message) {
		this.clients().stream()
		.filter(v -> !Objects.equals(from, v.clientId()))
		.forEach(v -> v.push(message));
	}
	
	/**
	 * 授权终端广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(Client from, Message message) {
		this.clients().stream()
		.filter(v -> v.instance() != from)
		.forEach(v -> v.push(message));
	}
	
	/**
	 * @param instance 终端实例
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
	 * @param clientId 终端标识
	 * 
	 * @return 授权终端列表
	 */
	public List<Client> clients(String clientId) {
		return this.clients().stream()
			.filter(v -> Objects.equals(clientId, v.clientId()))
			.toList();
	}
	
	/**
	 * @return 所有授权终端列表
	 */
	public List<Client> clients() {
		return this.clients.stream()
			.filter(Client::authorized)
			.toList();
	}
	
	/**
	 * @param instance 终端实例
	 * 
	 * @return 终端状态
	 */
	public ClientStatus status(AutoCloseable instance) {
		final Client client = this.client(instance);
		return client == null ? null : client.status();
	}
	
	/**
	 * @param clientId 终端标识
	 * 
	 * @return 授权终端状态列表
	 */
	public List<ClientStatus> status(String clientId) {
		return this.clients(clientId).stream()
			.map(Client::status)
			.toList();
	}

	/**
	 * @return 所有授权终端状态列表
	 */
	public List<ClientStatus> status() {
		return this.clients().stream()
			.map(Client::status)
			.toList();
	}

	/**
	 * 推送消息
	 * 
	 * @param instance 终端实例
	 * @param message 消息
	 */
	public void push(AutoCloseable instance, Message message) {
		final Client client = this.client(instance);
		if(client == null) {
			log.warn("推送消息终端无效：{}-{}", instance, message);
			return;
		}
		client.push(message);
	}
	
	/**
	 * 关闭终端
	 * 
	 * @param instance 终端实例
	 */
	public void close(AutoCloseable instance) {
		final Client client = this.client(instance);
		try {
			if(client != null) {
				client.close();
			} else {
				instance.close();
			}
		} catch (Exception e) {
			log.error("关闭终端异常：{}", instance, e);
		} finally {
			if(client != null) {
				// 移除管理
				this.clients.remove(client);
				// 关闭事件
				this.applicationContext.publishEvent(new ClientCloseEvent(client));
			}
		}
	}
	
	/**
	 * 定时关闭超时终端
	 */
	private void closeTimeout() {
	    final int oldSize = this.clients.size();
		this.clients.stream()
		.filter(v -> !v.authorized())
		.filter(v -> v.timeout(this.taoyaoProperties.getTimeout()))
		.forEach(v -> {
			log.debug("关闭超时终端：{}", v);
			this.close(v);
		});
		final int newSize = this.clients.size();
		log.debug("定时关闭超时终端：{}", newSize - oldSize);
	}

}
