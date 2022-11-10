package com.acgist.taoyao.signal.protocol;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.service.IdService;

/**
 * 信令协议适配器
 * 
 * @author acgist
 */
public abstract class ProtocolAdapter implements Protocol {

	@Autowired
	private IdService idService;
	@Autowired
	protected TaoyaoProperties taoyaoProperties;
	
	/**
	 * 信令协议标识
	 */
	protected final Integer protocol;

	protected ProtocolAdapter(Integer protocol) {
		this.protocol = protocol;
	}
	
	@Override
	public Integer protocol() {
		return this.protocol;
	}
	
	@Override
	public Message build() {
		final Header header = Header.builder()
			.v(this.taoyaoProperties.getVersion())
			.id(this.idService.id())
			.pid(this.protocol)
			.build();
		final Message message = Message.builder()
			.header(header)
			.build();
		return message;
	}
	
}
