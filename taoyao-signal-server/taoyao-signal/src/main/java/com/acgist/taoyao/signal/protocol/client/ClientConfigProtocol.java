package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.Constant;
import com.acgist.taoyao.boot.property.MediaProperties;
import com.acgist.taoyao.boot.property.WebrtcProperties;
import com.acgist.taoyao.boot.utils.DateUtils.DateTimeStyle;
import com.acgist.taoyao.signal.client.Client;
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
        "time": "系统时间（yyyyMMddHHmmss）",
        "media": "媒体配置",
        "webrtc": "WebRTC配置"
    }
    """,
    flow = "终端-[终端注册]>信令服务->终端"
)
public class ClientConfigProtocol extends ProtocolClientAdapter {

	public static final String SIGNAL = "client::config";
	
	@Autowired
	private MediaProperties mediaProperties;
	@Autowired
	private WebrtcProperties webrtcProperties;
	
	public ClientConfigProtocol() {
		super("终端配置信令", SIGNAL);
	}

	@Override
	public void execute(String clientId, Map<?, ?> body, Client client, Message message) {
		// 忽略
	}
	
	@Override
	public Message build() {
		final Message message = super.build();
		message.setBody(Map.of(
			// 系统时间
			Constant.TIME, DateTimeStyle.YYYYMMDDHH24MMSS.getDateTimeFormatter().format(LocalDateTime.now()),
			Constant.MEDIA, this.mediaProperties,
			Constant.WEBRTC, this.webrtcProperties
		));
		return message;
	}

}
