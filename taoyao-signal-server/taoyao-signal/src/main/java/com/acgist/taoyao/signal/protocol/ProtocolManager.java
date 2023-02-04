package com.acgist.taoyao.signal.protocol;

import java.net.http.WebSocket;
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
import com.acgist.taoyao.signal.protocol.platform.ErrorProtocol;
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
	private Map<Integer, Protocol> protocolMapping = new ConcurrentHashMap<>();
	
	@Autowired
	private ApplicationContext context;
	@Autowired
	private ErrorProtocol errorProtocol;
	@Autowired
	private SecurityService securityService;
	@Autowired
	private ClientSessionManager clientSessionManager;
	
	@PostConstruct
	public void init() {
		final Map<String, Protocol> map = this.context.getBeansOfType(Protocol.class);
		map.entrySet().stream()
		.sorted((a, z) -> Integer.compare(a.getValue().pid(), z.getValue().pid()))
		.forEach(e -> {
			final String k = e.getKey();
			final Protocol v = e.getValue();
			final Integer pid = v.pid();
			final String name = v.name();
			if(this.protocolMapping.containsKey(pid)) {
				throw MessageCodeException.of("存在重复信令协议：" + pid);
			}
			log.info("注册信令协议：{}-{}-{}", pid, name, k);
			this.protocolMapping.put(pid, v);
		});
	}
	
	/**
	 * @param pid 信令标识
	 * 
	 * @return 信令
	 */
	public Protocol protocol(Integer pid) {
		return this.protocolMapping.get(pid);
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
			session.push(this.errorProtocol.build("消息格式错误（解析失败）"));
			return;
		}
		final Header header = value.getHeader();
		if(header == null) {
			log.warn("消息格式错误（没有头部）：{}", message);
			session.push(this.errorProtocol.build("消息格式错误（没有头部）"));
			return;
		}
		final String v = header.getV();
		final String id = header.getId();
		final String sn = header.getSn();
		final Integer pid = header.getPid();
		if(v == null || id == null || sn == null || pid == null) {
			log.warn("消息格式错误（缺失头部关键参数）：{}", message);
			session.push(this.errorProtocol.build("消息格式错误（缺失头部关键参数）"));
			return;
		}
		// 设置缓存ID
		this.errorProtocol.set(id);
		// 开始处理协议
		final Protocol protocol = this.protocolMapping.get(pid);
		if(protocol == null) {
			log.warn("不支持的信令协议：{}", message);
			session.push(this.errorProtocol.build("不支持的信令协议：" + pid));
			return;
		}
		if(protocol instanceof ClientRegisterProtocol) {
			protocol.execute(sn, value, session);
		} else if(this.securityService.authenticate(value, session, protocol)) {
			protocol.execute(sn, value, session);
		} else {
			log.warn("终端会话没有授权：{}", message);
			session.push(this.errorProtocol.build(MessageCode.CODE_3401, "终端会话没有授权"));
		}
	}
	
}
