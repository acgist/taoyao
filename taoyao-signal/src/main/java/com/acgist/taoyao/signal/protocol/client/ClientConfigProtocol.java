package com.acgist.taoyao.signal.protocol.client;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.MediaProperties;
import com.acgist.taoyao.boot.property.WebrtcProperties;
import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.boot.utils.DateUtils.DateTimeStyle;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.ProtocolAdapter;

/**
 * 下发配置信令
 * 
 * @author acgist
 */
@Protocol
public class ClientConfigProtocol extends ProtocolAdapter {

	public static final Integer PID = 2004;
	
	@Autowired
	private MediaProperties mediaProperties;
	@Autowired
	private WebrtcProperties webrtcProperties;
	
	public ClientConfigProtocol() {
		super(PID, "信令协议标识");
	}

	@Override
	public void execute(String sn, Message message, ClientSession session) {
		// 忽略
	}
	
	@Override
	public Message build() {
		final Message message = super.build();
		message.setBody(Map.of(
			"time", DateUtils.format(LocalDateTime.now(), DateTimeStyle.YYYYMMDDHH24MMSS),
			"media", this.mediaProperties,
			"webrtc", this.webrtcProperties
		));
		return message;
	}

}
