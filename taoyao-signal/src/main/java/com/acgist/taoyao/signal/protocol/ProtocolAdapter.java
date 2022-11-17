package com.acgist.taoyao.signal.protocol;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

/**
 * 信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolAdapter implements Protocol {

	@Autowired
	protected IdService idService;
	@Autowired
	protected ApplicationContext context;
	@Autowired
	protected TaoyaoProperties taoyaoProperties;
	
	/**
	 * 信令标识
	 */
	protected final Integer pid;
	/**
	 * 信令名称
	 */
	protected final String name;

	protected ProtocolAdapter(Integer pid, String name) {
		this.pid = pid;
		this.name = name;
	}
	
	@Override
	public Integer pid() {
		return this.pid;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public <E extends ApplicationEventAdapter> void publishEvent(E event) {
		this.context.publishEvent(event);
	}
	
	@Override
	public Message build() {
		return this.build(null, null, null, null);
	}
	
	@Override
	public Message build(Object body) {
		return this.build(null, null, null, body);
	}
	
	@Override
	public Message build(MessageCode code, Object body) {
		return this.build(null, code, null, body);
	}
	
	@Override
	public Message build(MessageCode code, String message, Object body) {
		return this.build(null, code, message, body);
	}
	
	@Override
	public Message build(String id, MessageCode code, String message, Object body) {
		if(StringUtils.isEmpty(id)) {
			id = this.idService.buildIdToString();
		}
		final Header header = Header.builder()
			.v(this.taoyaoProperties.getVersion())
			.id(id)
			.pid(this.pid)
			.build();
		final Message build = Message.builder()
			.header(header)
			.build();
		if(code != null) {
			build.setCode(code);
		}
		if(StringUtils.isNotEmpty(message)) {
			build.setMessage(message);
		}
		if(body != null) {
			build.setBody(body);
		}
		return build;
	}
	
}
