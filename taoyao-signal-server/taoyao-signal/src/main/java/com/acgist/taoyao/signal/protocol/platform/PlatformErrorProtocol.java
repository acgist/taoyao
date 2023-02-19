package com.acgist.taoyao.signal.protocol.platform;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 平台异常信令
 * 
 * @author acgist
 */
@Protocol
@Description(flow = "终端->信令服务->终端")
public class PlatformErrorProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "platform::error";
	
	/**
	 * 请求ID缓存
	 */
	private ThreadLocal<String> idLocal = new InheritableThreadLocal<>();
	
	public PlatformErrorProtocol() {
		super("平台异常信令", SIGNAL);
	}

	/**
	 * @param id 请求ID
	 */
	public void set(String id) {
		this.idLocal.set(id);
	}
	
	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
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

	@Override
	public Message build(String id, MessageCode code, String message, Object body) {
		final String oldId = this.idLocal.get();
		if(oldId == null) {
			id = this.idService.buildIdToString();
		} else {
			id = oldId;
			this.idLocal.remove();
		}
		// 默认设置失败状态
		return super.build(id, code == null ? MessageCode.CODE_9999 : code, message, body);
	}
	
}
