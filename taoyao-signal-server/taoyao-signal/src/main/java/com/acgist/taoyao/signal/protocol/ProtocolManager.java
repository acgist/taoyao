package com.acgist.taoyao.signal.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.protocol.client.ClientRegisterProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;
import com.acgist.taoyao.signal.service.SecurityService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class ProtocolManager {

	/**
	 * 协议映射
	 */
	private Map<String, Protocol> protocolMapping = new ConcurrentHashMap<>();
	
	@Autowired
	private ApplicationContext context;
	@Autowired
	private SecurityService securityService;
	@Autowired
	private ClientSessionManager clientSessionManager;
	@Autowired
	private PlatformErrorProtocol platformErrorProtocol;
	
	@PostConstruct
	public void init() {
		final Map<String, Protocol> map = this.context.getBeansOfType(Protocol.class);
		map.entrySet().stream()
		.sorted((a, z) -> a.getValue().signal().compareTo(z.getValue().signal()))
		.forEach(e -> {
			final String k = e.getKey();
			final Protocol v = e.getValue();
			final String name = v.name();
			final String signal = v.signal();
			if(this.protocolMapping.containsKey(signal)) {
				throw MessageCodeException.of("存在重复信令协议：" + signal);
			}
			log.info("注册信令协议：{} - {} - {}", String.format("%-32s", signal), String.format("%-32s", k), name);
			this.protocolMapping.put(signal, v);
		});
	}
	
	/**
	 * @param signal 信令标识
	 * 
	 * @return 信令
	 */
	public Protocol protocol(String signal) {
		return this.protocolMapping.get(signal);
	}
	
	/**
	 * 执行信令消息
	 * 
	 * @param message 信令消息
	 * @param instance 会话实例
	 */
	public void execute(String message, AutoCloseable instance) {
		log.debug("执行信令消息：{}", message);
		final ClientSession session = this.clientSessionManager.session(instance);
		// 验证请求
		final Message value = JSONUtils.toJava(message, Message.class);
		if(value == null) {
			log.warn("消息格式错误（解析失败）：{}", message);
			session.push(this.platformErrorProtocol.build("消息格式错误（解析失败）"));
			return;
		}
		final Header header = value.getHeader();
		if(header == null) {
			log.warn("消息格式错误（没有头部）：{}", message);
			session.push(this.platformErrorProtocol.build("消息格式错误（没有头部）"));
			return;
		}
		final String v = header.getV();
		final String id = header.getId();
		final String sn = header.getSn();
		final String signal = header.getSignal();
		if(v == null || id == null || sn == null || signal == null) {
			log.warn("消息格式错误（缺失头部关键参数）：{}", message);
			session.push(this.platformErrorProtocol.build("消息格式错误（缺失头部关键参数）"));
			return;
		}
		// 设置缓存ID
		this.platformErrorProtocol.set(id);
		// 开始处理协议
		final Protocol protocol = this.protocolMapping.get(signal);
		if(protocol == null) {
			log.warn("不支持的信令协议：{}", message);
			session.push(this.platformErrorProtocol.build("不支持的信令协议：" + signal));
			return;
		}
		if(protocol instanceof ClientRegisterProtocol) {
			protocol.execute(sn, value, session);
		} else if(this.securityService.authenticate(value, session, protocol)) {
			protocol.execute(sn, value, session);
		} else {
			log.warn("终端会话没有授权：{}", message);
			session.push(this.platformErrorProtocol.build(MessageCode.CODE_3401, "终端会话没有授权"));
		}
	}
	
}
