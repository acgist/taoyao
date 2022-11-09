package com.acgist.taoyao.signal.session;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.websocket.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.signal.message.Message;
import com.acgist.taoyao.signal.session.websocket.SessionWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 会话管理
 * 
 * @author acgist
 */
@Slf4j
@Service
public class SessionManager {
	
	@Autowired
	private TaoyaoProperties taoyaoProperties;
	
	/**
	 * 没有授权会话
	 */
	private Map<Session, Long> unauthorized = new ConcurrentHashMap<>();
	/**
	 * 授权会话列表
	 */
	private List<SessionWrapper> sessions = new CopyOnWriteArrayList<>();

	@Scheduled(cron = "${taoyao.scheduled.session:0 * * * * ?}")
	public void scheduled() {
		this.closeTimeoutSession();
	}
	
	/**
	 * 存入没有授权会话，定时清除没有授权会话。
	 * 
	 * @param session 会话
	 */
	public void open(Session session) {
		this.unauthorized.put(session, System.currentTimeMillis());
	}

	/**
	 * @param session 会话
	 * 
	 * @return 会话包装器
	 */
	public SessionWrapper getWrapper(Session session) {
		return this.sessions.stream()
			.filter(v -> v.matchSession(session))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * 认证会话
	 * 
	 * @param sn 终端标识
	 * @param session 会话
	 */
	public void authorized(String sn, Session session) {
		this.unauthorized.remove(session);
		final SessionWrapper wrapper = new SessionWrapper();
		wrapper.setSn(sn);
		wrapper.setSession(session);
		this.sessions.add(wrapper);
	}
	
	/**
	 * 单播消息
	 * 
	 * @param to 接收终端
	 * @param message 消息
	 */
	public void unicast(String to, Message message) {
		this.sessions.stream().filter(v -> v.matchSn(to)).forEach(v -> {
			message.getHeader().setSn(v.getSn());
			message.getHeader().setVersion(this.taoyaoProperties.getVersion());
			v.send(message);
		});
	}
	
	/**
	 * 广播消息
	 * 
	 * @param message 消息
	 */
	public void broadcast(Message message) {
		this.sessions.forEach(v -> {
			message.getHeader().setSn(v.getSn());
			message.getHeader().setVersion(this.taoyaoProperties.getVersion());
			v.send(message);
		});
	}
	
	/**
	 * 广播消息
	 * 
	 * @param from 发送终端
	 * @param message 消息
	 */
	public void broadcast(String from, Message message) {
		this.sessions.stream().filter(v -> v.matchNoneSn(from)).forEach(v -> {
			message.getHeader().setSn(v.getSn());
			message.getHeader().setVersion(this.taoyaoProperties.getVersion());
			v.send(message);
		});
	}
	
	/**
	 * 关闭会话
	 * 
	 * @param session 会话
	 */
	public void close(Session session) {
		final SessionWrapper wrapper = this.getWrapper(session);
		if(wrapper != null) {
			// TODO：退出房间
			// TODO：退出帐号
			// 移除
			this.sessions.remove(wrapper);
		}
		try {
			session.close();
		} catch (IOException e) {
			log.error("关闭会话异常", e);
		}
	}
	
	/**
	 * 定时关闭超时会话
	 */
	private void closeTimeoutSession() {
		log.debug("定时关闭超时会话");
		final Iterator<Entry<Session, Long>> iterator = this.unauthorized.entrySet().iterator();
		while(iterator.hasNext()) {
			final Entry<Session, Long> next = iterator.next();
			final Long last = next.getValue();
			final Session session = next.getKey();
			if(System.currentTimeMillis() - last > this.taoyaoProperties.getTimeout()) {
				log.debug("关闭超时会话：{}", session);
				this.close(session);
			}
		}
	}
	
}
