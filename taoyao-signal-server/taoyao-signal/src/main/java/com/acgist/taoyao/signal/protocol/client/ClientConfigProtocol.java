package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.config.MediaProperties;
import com.acgist.taoyao.boot.config.WebrtcProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.boot.utils.DateUtils.DateTimeStyle;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.ClientConfigEvent;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

/**
 * 终端配置信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "media": "媒体配置（可选）",
        "webrtc": "WebRTC配置（可选）",
        "datetime": "日期时间（yyyyMMddHHmmss）"
    }
    """,
    flow = "终端=[终端注册]>信令服务->终端"
)
public class ClientConfigProtocol extends ProtocolClientAdapter implements ApplicationListener<ClientConfigEvent> {

	public static final String SIGNAL = "client::config";
	
	private final MediaProperties mediaProperties;
	private final WebrtcProperties webrtcProperties;
	
	public ClientConfigProtocol(MediaProperties mediaProperties, WebrtcProperties webrtcProperties) {
		super("终端配置信令", SIGNAL);
		this.mediaProperties = mediaProperties;
		this.webrtcProperties = webrtcProperties;
	}

	@Async
	@Override
    public void onApplicationEvent(ClientConfigEvent event) {
	    final Client client = event.getClient();
	    final ClientType clientType = client.clientType();
	    client.push(this.build(clientType));
    }
	
	@Override
	public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
	    client.push(this.build(clientType));
	}
	
	/**
	 * @param clientType 终端类型
	 * 
	 * @return 消息
	 */
	public Message build(ClientType clientType) {
		final Message message = super.build();
		final Map<String, Object> config = new HashMap<>();
		// 日期时间
		config.put(Constant.DATETIME, DateUtils.format(LocalDateTime.now(), DateTimeStyle.YYYYMMDDHH24MMSS));
		// Web、摄像头：媒体配置
		if(clientType.mediaClient()) {
		    config.put(Constant.MEDIA, this.mediaProperties);
		    config.put(Constant.WEBRTC, this.webrtcProperties);
		} else {
		    this.logNoAdapter(clientType);
		}
		message.setBody(config);
		return message;
	}
	
}
