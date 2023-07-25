package com.acgist.taoyao.signal.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import lombok.extern.slf4j.Slf4j;

/**
 * 信令管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class ProtocolManager {
    
    private final ClientManager clientManager;
    private final SecurityService securityService;
    private final ApplicationContext applicationContext;
    private final PlatformErrorProtocol platformErrorProtocol;
    
    /**
     * 信令映射
     */
    private final Map<String, Protocol> protocolMapping;

	public ProtocolManager(ClientManager clientManager, SecurityService securityService, ApplicationContext applicationContext, PlatformErrorProtocol platformErrorProtocol) {
        this.clientManager = clientManager;
        this.securityService = securityService;
        this.applicationContext = applicationContext;
        this.platformErrorProtocol = platformErrorProtocol;
        this.protocolMapping = new ConcurrentHashMap<>();
    }
	
	/**
	 * 加载信令映射
	 */
	public void init() {
	    this.applicationContext.getBeansOfType(Protocol.class).entrySet().stream()
		.sorted((a, z) -> a.getValue().signal().compareTo(z.getValue().signal()))
		.forEach(e -> {
			final String key = e.getKey();
			final Protocol value = e.getValue();
			final String name = value.name();
			final String signal = value.signal();
			if(this.protocolMapping.containsKey(signal)) {
				throw MessageCodeException.of("存在重复信令协议：" + signal);
			}
			if(log.isDebugEnabled()) {
			    log.debug("注册信令协议：{} - {} - {}", String.format("%-36s", signal), String.format("%-36s", key), name);
			}
			this.protocolMapping.put(signal, value);
		});
	    log.info("当前注册信令数量：{}", this.protocolMapping.size());
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
		final Client client = this.clientManager.getClients(instance);
		if(client == null) {
			log.warn("信令终端无效：{}-{}", instance, content);
			return;
		}
		// 验证请求
		final Message message = JSONUtils.toJava(content, Message.class);
		if(message == null) {
			log.warn("信令消息格式错误（解析失败）：{}", content);
			client.push(this.platformErrorProtocol.build(MessageCode.CODE_1002, "信令消息格式错误（解析失败）"));
			return;
		}
		final Header header = message.getHeader();
		if(header == null) {
			log.warn("信令消息格式错误（没有头部）：{}", content);
			client.push(this.platformErrorProtocol.build(MessageCode.CODE_1002, "信令消息格式错误（没有头部）"));
			return;
		}
		final String v = header.getV();
		final Long id = header.getId();
		final String signal = header.getSignal();
		// 设置缓存ID
		this.platformErrorProtocol.set(id);
		if(v == null || id == null || signal == null) {
			log.warn("信令消息格式错误（缺失头部关键参数）：{}", content);
			client.push(this.platformErrorProtocol.build(MessageCode.CODE_1002, "信令消息格式错误（缺失头部关键参数）"));
			return;
		}
		// 开始处理协议
		final Protocol protocol = this.protocolMapping.get(signal);
		if(protocol == null) {
			log.warn("不支持的信令协议：{}", content);
			client.push(this.platformErrorProtocol.build(MessageCode.CODE_3415, "不支持的信令协议：" + signal));
			return;
		}
		if(log.isDebugEnabled()) {
		    log.debug("执行信令消息：{} - {}", client.getClientId(), content);
		}
		if(protocol instanceof ClientRegisterProtocol) {
			protocol.execute(client, message);
		} else if(this.securityService.authenticate(client, message, protocol)) {
		    if(client.response(id, message)) {
		        // 响应消息不做其他处理
		    } else {
		        protocol.execute(client, message);
		    }
		} else {
			log.warn("终端没有授权：{}", content);
			client.push(this.platformErrorProtocol.build(MessageCode.CODE_3401, "终端没有授权"));
		}
	}
	
}
