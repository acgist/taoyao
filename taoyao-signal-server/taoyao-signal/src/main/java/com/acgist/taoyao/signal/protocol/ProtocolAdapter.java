package com.acgist.taoyao.signal.protocol;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.signal.MapBodyGetter;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;
import com.acgist.taoyao.signal.media.MediaClientManager;
import com.acgist.taoyao.signal.media.RoomManager;

/**
 * 信令适配器
 * 
 * @author acgist
 */
public abstract class ProtocolAdapter implements Protocol, MapBodyGetter {

	@Autowired
	protected IdService idService;
	@Autowired
	protected RoomManager roomManager;
	@Autowired
	protected ClientManager clientManager;
	@Autowired
	protected TaoyaoProperties taoyaoProperties;
	@Autowired
	protected ApplicationContext applicationContext;
	@Autowired
	protected MediaClientManager mediaClientManager;
	
	/**
	 * 信令名称
	 */
	protected final String name;
	/**
	 * 信令标识
	 */
	protected final String signal;

	protected ProtocolAdapter(String name, String signal) {
		this.name = name;
		this.signal = signal;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public String signal() {
		return this.signal;
	}
	
	@Override
	public <E extends ApplicationEventAdapter> void publishEvent(E event) {
		this.applicationContext.publishEvent(event);
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
	public Message build(String message, Object body) {
		return this.build(null, null, message, body);
	}
	
	@Override
	public Message build(MessageCode code, String message, Object body) {
		return this.build(null, code, message, body);
	}
	
	@Override
	public Message build(String id, MessageCode code, String message, Object body) {
	    // 消息标识
		if(StringUtils.isEmpty(id)) {
			id = this.idService.buildIdToString();
		}
		// 消息头部
		final Header header = Header.builder()
			.v(this.taoyaoProperties.getVersion())
			.id(id)
			.signal(this.signal)
			.build();
		final Message build = Message.builder().build();
		// 设置状态编码、状态描述：默认成功
		build.setCode(code == null ? MessageCode.CODE_0000 : code, message);
		// 设置消息头部
		build.setHeader(header);
		// 设置消息主体
		build.setBody(body);
		return build;
	}
	
}
