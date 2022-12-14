package com.acgist.taoyao.signal.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.client.ClientCloseEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 会话管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class ClientSessionManager {
	
	@Autowired
	private ApplicationContext context;
	@Autowired
	private TaoyaoProperties taoyaoProperties;
	
	/**
	 * 会话列表
	 */
	private List<ClientSession> sessions = new CopyOnWriteArrayList<>();

	@Scheduled(cron = "${taoyao.scheduled.session:0 * * * * ?}")
	public void scheduled() {
		this.closeTimeout();
	}
	
	/**
	 * @param session 会话
	 */
	public void open(ClientSession session) {
		this.sessions.add(session);
	}
	
	/**
	 * @param instance 会话实例
	 * 
	 * @return 会话
	 */
	public ClientSession session(AutoCloseable instance) {
		return this.sessions.stream()
			.filter(v -> v.matchInstance(instance))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * 单播消息
	 * 
	 * @param to 接收终端
	 * @param message 消息
	 */
	public void unicast(String to, Message message) {
		this.sessions().stream()
		.filter(v -> v.filterSn(to))
		.forEach(v -> {
			message.getHeader().setSn(v.sn());
			v.push(message);
		});
	}
	
	/**
	 * 广播消息
	 * 
	 * @param message 消息
	 */
	public void broadcast(Message message) {
		this.sessions().forEach(v -> {
			message.getHeader().setSn(v.sn());
			v.push(message);
		});
	}
	
	/**
	 * 广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(String from, Message message) {
		this.sessions().stream().
		filter(v -> v.filterNoneSn(from))
		.forEach(v -> {
			message.getHeader().setSn(v.sn());
			v.push(message);
		});
	}
	
	/**
	 * @param sn 终端标识
	 * 
	 * @return 终端会话
	 */
	public ClientSession session(String sn) {
		return this.sessions().stream()
			.filter(v -> StringUtils.equals(sn, v.sn()))
			.findFirst()
			.orElse(null);
	}

	/**
	 * @param sn 终端标识
	 * 
	 * @return 终端状态
	 */
	public ClientSessionStatus status(String sn) {
		final ClientSession session = this.session(sn);
		return session == null ? null : session.status();
	}
	
	/**
	 * @return 所有终端会话
	 */
	public List<ClientSession> sessions() {
		return this.sessions.stream()
			.filter(ClientSession::authorized)
			.toList();
	}

	/**
	 * @return 所有终端状态
	 */
	public List<ClientSessionStatus> status() {
		return this.sessions().stream()
			.map(ClientSession::status)
			.toList();
	}
	
	/**
	 * 关闭会话
	 * 
	 * @param instance 会话实例
	 */
	public void close(AutoCloseable instance) {
		final ClientSession session = this.session(instance);
		try {
			if(session != null) {
				session.close();
			} else {
				instance.close();
			}
		} catch (Exception e) {
			log.error("关闭会话异常", e);
		} finally {
			if(session != null) {
				// 移除管理
				this.sessions.remove(session);
				// 关闭事件
				this.context.publishEvent(new ClientCloseEvent(null, session));
			}
		}
	}
	
	/**
	 * 定时关闭超时会话
	 */
	private void closeTimeout() {
		log.debug("定时关闭超时会话");
		this.sessions.stream()
		.filter(v -> !v.authorized())
		.filter(v -> v.timeout(this.taoyaoProperties.getTimeout()))
		.forEach(v -> {
			log.debug("关闭超时会话：{}", v);
			this.close(v);
		});
	}

}
