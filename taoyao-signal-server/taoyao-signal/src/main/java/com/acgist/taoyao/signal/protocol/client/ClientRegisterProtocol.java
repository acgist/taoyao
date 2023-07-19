package com.acgist.taoyao.signal.protocol.client;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientStatus;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.client.ClientConfigEvent;
import com.acgist.taoyao.signal.event.client.ClientOnlineEvent;
import com.acgist.taoyao.signal.event.room.RoomCreateEvent;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;
import com.acgist.taoyao.signal.service.SecurityService;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端注册信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
@Description(
    body = """
    {
        "username": "信令用户",
        "password": "信令密码",
        "name": "终端名称",
        "clientId": "终端ID",
        "clientType": "终端类型",
        "latitude": 纬度,
        "longitude": 经度,
        "humidity": 湿度,
        "temperature": 温度,
        "signal": 信号强度（0~100）,
        "battery": 电池电量（0~100）,
        "alarming": 是否发生告警（true|false）,
        "charging": 是否正在充电（true|false）,
        "recording": 是否正在录像（true|false）,
        "lastHeartbeat": "最后心跳时间",
        "status": {更多状态},
        "config": {更多配置}
    }
    """,
    flow = {
        "终端=>信令服务->终端",
        "终端=>信令服务-[终端配置]>终端",
        "终端=>信令服务-[终端上线])终端"
    }
)
public class ClientRegisterProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::register";
	
	private final SecurityService securityService;
	
	public ClientRegisterProtocol(SecurityService securityService) {
		super("终端注册信令", SIGNAL);
		this.securityService = securityService;
	}

    @Override
	public void execute(String nullClientId, ClientType nullClientType, Client client, Message message, Map<String, Object> body) {
		final String clientId = MapUtils.get(body, Constant.CLIENT_ID);
		final String username = MapUtils.get(body, Constant.USERNAME);
		final String password = MapUtils.get(body, Constant.PASSWORD);
		if(this.securityService.authenticate(username, password)) {
		    final Client oldClient = this.clientManager.clients(clientId);
		    if(oldClient != null) {
		        log.debug("终端已经存在（注销旧的终端）：{}", clientId);
		        CloseableUtils.close(oldClient);
		    }
			log.info("终端注册：{}", clientId);
			client.authorize(clientId);
			message.setCode(MessageCode.CODE_0000);
		} else {
		    throw MessageCodeException.of(MessageCode.CODE_3401, "注册失败");
		}
		final ClientType clientType = ClientType.of(MapUtils.get(body, Constant.CLIENT_TYPE));
		// 注册响应消息
		final Message response = message.cloneWithoutBody();
		response.setBody(Map.of(Constant.INDEX, this.idService.buildClientIndex()));
		client.push(response);
		// 设置终端状态
		this.buildStatus(clientId, clientType, client, body);
        // 终端配置事件
		this.publishEvent(new ClientConfigEvent(client));
        // 终端上线事件
        this.publishEvent(new ClientOnlineEvent(client));
        // 媒体服务注册：创建房间事件
        if(clientType.mediaServer()) {
            this.publishEvent(new RoomCreateEvent(client));
        }
	}
	
	/**
	 * @param clientId 终端ID
	 * @param clientType 终端类型
	 * @param client 终端
	 * @param body 消息主体
	 * 
	 * @return 终端状态
	 */
	private ClientStatus buildStatus(String clientId, ClientType clientType, Client client, Map<String, Object> body) {
        final ClientStatus status = client.getStatus();
        status.setIp(client.getIP());
        status.setName(MapUtils.get(body, Constant.NAME));
        status.setClientId(clientId);
        status.setClientType(clientType);
        status.copy(body);
        return status;
	}
	
}
