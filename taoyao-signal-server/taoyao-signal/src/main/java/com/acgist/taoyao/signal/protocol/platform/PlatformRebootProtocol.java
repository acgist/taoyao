package com.acgist.taoyao.signal.protocol.platform;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.config.ScriptProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ControlProtocol;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 重启平台信令
 * 
 * @author acgist
 */
@Slf4j
@Description(flow = "终端->信令服务+)终端")
public class PlatformRebootProtocol extends ProtocolClientAdapter implements ControlProtocol {

	public static final String SIGNAL = "platform::reboot";
	
	@Autowired
	private ScriptProperties scriptProperties;
	
	public PlatformRebootProtocol() {
		super("重启平台信令", SIGNAL);
	}
	
	/**
	 * 执行命令信令
	 */
	public void execute() {
        log.info("重启平台");
        this.clientManager.broadcast(this.build());
        ScriptUtils.execute(this.scriptProperties.getPlatformReboot());
    }

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		log.info("重启平台：{}", clientId);
		this.clientManager.broadcast(message);
		ScriptUtils.execute(this.scriptProperties.getPlatformReboot());
	}

}