package com.acgist.taoyao.signal.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Service;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.signal.protocol.client.RegisterProtocol;
import com.acgist.taoyao.signal.session.ClientSession;
import com.acgist.taoyao.signal.session.ClientSessionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 协议管理
 * 
 * @author acgist
 */
@Slf4j
@Service
public class ProtocolManager {

	/**
	 * 协议映射
	 */
	private Map<Integer, Protocol> protocolMapping = new ConcurrentHashMap<>();
	
	@Autowired
	private ApplicationContext context;
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	@PostConstruct
	public void init() {
		final Map<String, Protocol> map = this.context.getBeansOfType(Protocol.class);
		map.forEach((k, v) -> {
			final Integer protocol = v.protocol();
			if(this.protocolMapping.containsKey(protocol)) {
				throw MessageCodeException.of("存在重复信令协议：" + protocol);
			}
			log.info("注册信令协议：{}-{}", protocol, k);
			this.protocolMapping.put(protocol, v);
		});
	}
	
	/**
	 * 执行信令消息
	 * 
	 * @param message 信令消息
	 * @param instance 会话实例
	 */
	public void execute(String message, AutoCloseable instance) {
		log.debug("执行信令消息：{}", message);
		if(StringUtils.isEmpty(message)) {
			log.warn("消息为空：{}", message);
			return;
		}
		final Message value = JSONUtils.toJava(message, Message.class);
		final Header header = value.getHeader();
		if(header == null) {
			log.warn("消息格式错误（没有头部）：{}", message);
			return;
		}
		final String sn = header.getSn();
		final Integer pid = header.getPid();
		if(sn == null || pid == null) {
			log.warn("消息格式错误（没有SN或者PID）：{}", message);
			return;
		}
		final Protocol protocol = this.protocolMapping.get(pid);
		if(protocol == null) {
			log.warn("不支持的信令协议：{}", message);
			return;
		}
		ApplicationEvent event = null;
		final ClientSession session = this.clientSessionManager.session(instance);
		if(session != null && protocol instanceof RegisterProtocol) {
			event = protocol.execute(sn, value, session);
		} else if(session != null) {
			event = protocol.execute(sn, value, session);
		} else {
			log.warn("会话没有权限：{}", message);
		}
		if(event != null) {
			this.context.publishEvent(event);
		}
	}

}
