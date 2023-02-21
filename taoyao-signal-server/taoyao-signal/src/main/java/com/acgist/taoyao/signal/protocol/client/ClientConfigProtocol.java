package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.config.MediaProperties;
import com.acgist.taoyao.boot.config.WebrtcProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.service.IpService;
import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.boot.utils.DateUtils.DateTimeStyle;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;
import com.acgist.taoyao.signal.wrapper.WebrtcPropertiesWrapper;

/**
 * 终端配置信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = """
    {
        "media": "媒体配置",
        "webrtc": "WebRTC配置",
        "datetime": "日期时间（yyyyMMddHHmmss）"
    }
    """,
    flow = "终端-[终端注册]>信令服务->终端"
)
public class ClientConfigProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::config";
	
	@Autowired
	private IpService ipService;
	@Autowired
	private MediaProperties mediaProperties;
	@Autowired
	private WebrtcProperties webrtcProperties;
	
	public ClientConfigProtocol() {
		super("终端配置信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
	    client.push(this.build(client));
	}
	
	/**
	 * @param client 终端
	 * 
	 * @return 信令消息
	 */
	public Message build(Client client) {
	    final String clientIp = client.ip();
		final Message message = this.build();
		final WebrtcPropertiesWrapper webrtcPropertiesWrapper = new WebrtcPropertiesWrapper(clientIp, this.ipService, this.webrtcProperties);
		message.setBody(Map.of(
			Constant.MEDIA, this.mediaProperties,
			Constant.WEBRTC, webrtcPropertiesWrapper,
			Constant.DATETIME, DateUtils.format(LocalDateTime.now(), DateTimeStyle.YYYYMMDDHH24MMSS)
		));
		return message;
	}
	
}
