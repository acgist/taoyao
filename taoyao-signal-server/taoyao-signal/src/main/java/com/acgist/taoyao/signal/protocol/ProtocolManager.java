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
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientManager;
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
	private ClientManager clientManager;
	@Autowired
	private SecurityService securityService;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private PlatformErrorProtocol platformErrorProtocol;
	
	@PostConstruct
	public void init() {
		final Map<String, Protocol> map = this.applicationContext.getBeansOfType(Protocol.class);
		map.entrySet().stream()
		.sorted((a, z) -> a.getValue().signal().compareTo(z.getValue().signal()))
		.forEach(e -> {
			final String key = e.getKey();
			final Protocol value = e.getValue();
			final String name = value.name();
			final String signal = value.signal();
			if(this.protocolMapping.containsKey(signal)) {
				throw MessageCodeException.of("存在重复信令协议：" + signal);
			}
			log.info("注册信令协议：{} - {} - {}", String.format("%-32s", signal), String.format("%-32s", key), name);
			this.protocolMapping.put(signal, value);
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
	 * @param content 信令消息
	 * @param instance 终端实例
	 */
	public void execute(String content, AutoCloseable instance) {
		log.debug("执行信令消息：{}", content);
		final Client client = this.clientManager.client(instance);
		if(client == null) {
			log.warn("信令终端无效：{}-{}", instance, content);
			return;
		}
		// 验证请求
		final Message message = JSONUtils.toJava(content, Message.class);
		if(message == null) {
			log.warn("信令消息格式错误（解析失败）：{}", content);
			client.push(this.platformErrorProtocol.build("信令消息格式错误（解析失败）"));
			return;
		}
		final Header header = message.getHeader();
		if(header == null) {
			log.warn("信令消息格式错误（没有头部）：{}", content);
			client.push(this.platformErrorProtocol.build("信令消息格式错误（没有头部）"));
			return;
		}
		final String v = header.getV();
		final String id = header.getId();
		final String signal = header.getSignal();
		// 设置缓存ID
		this.platformErrorProtocol.set(id);
		if(v == null || id == null || signal == null) {
			log.warn("信令消息格式错误（缺失头部关键参数）：{}", content);
			client.push(this.platformErrorProtocol.build("信令消息格式错误（缺失头部关键参数）"));
			return;
		}
		// 开始处理协议
		final Protocol protocol = this.protocolMapping.get(signal);
		if(protocol == null) {
			log.warn("不支持的信令协议：{}", content);
			client.push(this.platformErrorProtocol.build("不支持的信令协议：" + signal));
			return;
		}
		if(protocol instanceof ClientRegisterProtocol) {
			protocol.execute(client, message);
		} else if(this.securityService.authenticate(client, message, protocol)) {
			protocol.execute(client, message);
		} else {
			log.warn("终端没有授权：{}", content);
			client.push(this.platformErrorProtocol.build(MessageCode.CODE_3401, "终端没有授权"));
		}
	}
	
}
