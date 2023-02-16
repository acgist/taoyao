package com.acgist.taoyao.signal.protocol.platform;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 执行命令信令
 * 
 * @author acgist
 */
@Slf4j
public class PlatformScriptProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "platform::script";
	
	public PlatformScriptProtocol() {
		super("执行命令信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
	    final String script = this.get(body, Constant.SCRIPT);
	    final String result = ScriptUtils.execute(this.get(body, Constant.SCRIPT));
	    log.info("""
	        执行终端：{}
	        执行命令：{}
	        执行结果：{}
	        """, clientId, script, result);
	    message.setBody(Map.of(Constant.RESULT, result));
        client.push(message);
	}
	
}
