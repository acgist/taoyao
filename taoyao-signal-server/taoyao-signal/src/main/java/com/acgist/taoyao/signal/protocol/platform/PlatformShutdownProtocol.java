package com.acgist.taoyao.signal.protocol.platform;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.config.ScriptProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ControlProtocol;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 关闭平台信令
 * 
 * @author acgist
 */
@Slf4j
@Description(flow = "终端->信令服务+)终端")
public class PlatformShutdownProtocol extends ProtocolClientAdapter implements ControlProtocol {

	public static final String SIGNAL = "platform::shutdown";
	
	@Autowired
	private ScriptProperties scriptProperties;
	
	public PlatformShutdownProtocol() {
		super("关闭平台信令", SIGNAL);
	}

	/**
	 * 执行命令信令
	 */
	public void execute() {
	    log.info("关闭平台");
	    this.clientManager.broadcast(this.build());
	    this.shutdown();
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		log.info("关闭平台：{}", clientId);
		this.clientManager.broadcast(message);
		this.shutdown();
	}
	
	/**
	 * 关闭平台
	 */
	private void shutdown() {
        if(this.applicationContext instanceof ConfigurableApplicationContext context) {
            // API关闭
            if(context.isActive()) {
                // 如果需要完整广播可以设置延时
                context.close();
            } else {
                // 其他情况
            }
        } else {
            // 命令关闭
            ScriptUtils.execute(this.scriptProperties.getPlatformShutdown());
        }
	}

}