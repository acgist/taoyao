package com.acgist.taoyao.signal.protocol.platform;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 平台异常信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    flow = "终端->信令服务->终端"
)
public class PlatformErrorProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "platform::error";
	
	/**
	 * 请求ID缓存
	 */
	private ThreadLocal<Long> idLocal = new InheritableThreadLocal<>();
	
	public PlatformErrorProtocol() {
		super("平台异常信令", SIGNAL);
	}

	@Override
	public Message build(Long id, MessageCode code, String message, Object body) {
		final Long oldId = this.idLocal.get();
		if(oldId == null) {
			id = this.idService.buildId();
		} else {
			id = oldId;
			this.idLocal.remove();
		}
		// 默认设置失败状态
		return super.build(id, code == null ? MessageCode.CODE_9999 : code, message, body);
	}
	
	/**
	 * @param id 请求ID
	 */
	public void set(Long id) {
	    this.idLocal.set(id);
	}
	
	/**
	 * @param e 异常
	 * 
	 * @return 异常消息
	 */
	public Message build(Exception e) {
	    final Message message = super.build();
	    if(e instanceof MessageCodeException code) {
	        message.setCode(code.getCode(), code.getMessage());
	    }
	    message.setBody(e.getMessage());
	    return message;
	}
	
}
