package com.acgist.taoyao.signal.protocol.system;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.config.ScriptProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭系统信令
 * 
 * @author acgist
 */
@Slf4j
@Description(
    flow = {
        "信令服务+)终端",
        "终端->信令服务+)终端"
    }
)
public class SystemShutdownProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "system::shutdown";
	
	private final ScriptProperties scriptProperties;
	
	public SystemShutdownProtocol(ScriptProperties scriptProperties) {
		super("关闭系统信令", SIGNAL);
		this.scriptProperties = scriptProperties;
	}

	@Override
	public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
		log.info("关闭系统：{}", clientId);
		this.shutdown(message);
	}
	
	/**
	 * 执行命令信令
	 */
	public void execute() {
	    log.info("关闭系统");
	    this.shutdown(this.build());
	}
	
	/**
	 * 关闭系统
	 * 
	 * @param message 消息
	 */
	private void shutdown(Message message) {
	    this.clientManager.broadcast(message);
	    ScriptUtils.execute(this.scriptProperties.getSystemShutdown());
	}

}
