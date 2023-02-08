package com.acgist.taoyao.signal.protocol.platform;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 平台异常信令
 * 
 * @author acgist
 */
@Protocol
public class PlatformErrorProtocol extends ProtocolAdapter {

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
	public void execute(String sn, Message message, ClientSession session) {
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
		return super.build(id, code, message, body);
	}
	
}
