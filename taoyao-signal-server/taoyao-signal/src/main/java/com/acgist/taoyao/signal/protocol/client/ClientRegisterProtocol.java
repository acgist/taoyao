package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;
import com.acgist.taoyao.signal.service.SecurityService;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端注册信令
 * 如果需要验证终端授权自行实现
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
    {
        "clientId": "终端标识",
        "username": "信令用户",
        "password": "信令密码",
        "ip": "终端IP（选填）",
        "signal": 信号强度（0~100）,
        "battery": 电池电量（0~100）,
        "charging": 是否正在充电（true|false）
    }
    """,
    flow = { "终端->信令服务->终端", "终端->信令服务-[终端上线])终端" }
)
public class ClientRegisterProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::register";
	
	@Autowired
	private SecurityService securityService;
    @Autowired
    private ClientConfigProtocol configProtocol;
    @Autowired
    private ClientOnlineProtocol onlineProtocol;
	
	public ClientRegisterProtocol() {
		super("终端注册信令", SIGNAL);
	}

	@Override
	public void execute(String nullClientId, Map<?, ?> body, Client client, Message message) {
		final String clientId = this.get(body, Constant.CLIENT_ID);
		final String username = this.get(body, Constant.USERNAME);
		final String password = this.get(body, Constant.PASSWORD);
		// 如果需要终端鉴权在此实现
		if(this.securityService.authenticate(username, password)) {
			log.info("终端注册：{}", clientId);
			client.authorize(clientId);
			message.setCode(MessageCode.CODE_0000);
		} else {
		    throw MessageCodeException.of(MessageCode.CODE_3401, "注册失败");
		}
		// 推送消息
		client.push(message.cloneWithoutBody());
        // 下发配置
		client.push(this.configProtocol.build(client));
        // 终端状态
        final ClientStatus status = client.status();
        status.setClientId(clientId);
        status.setIp(StringUtils.defaultString(client.ip(), this.get(body, Constant.IP)));
        status.setSignal(this.get(body, Constant.SIGNAL));
        status.setBattery(this.get(body, Constant.BATTERY));
        status.setCharging(this.get(body, Constant.CHARGING));
        status.setLastHeartbeat(LocalDateTime.now());
        // 上线事件
        this.clientManager.broadcast(
            clientId,
            this.onlineProtocol.build(status)
        );
	}
	
}
