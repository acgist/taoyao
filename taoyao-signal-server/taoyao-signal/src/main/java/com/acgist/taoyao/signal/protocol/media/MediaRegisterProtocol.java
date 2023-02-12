package com.acgist.taoyao.signal.protocol.media;

import java.net.http.WebSocket;
import java.util.Map;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.MediaServerProperties;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.Constant;
import com.acgist.taoyao.signal.protocol.ProtocolMediaAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务注册信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
public class MediaRegisterProtocol extends ProtocolMediaAdapter {

	public static final String SIGNAL = "media::register";
	
	public MediaRegisterProtocol() {
		super("媒体服务注册信令", SIGNAL);
	}
	
	/**
	 * 创建信令消息
	 * 
	 * @param mediaServerProperties 媒体服务配置
	 * 
	 * @return 信令消息
	 */
	public Message build(MediaServerProperties mediaServerProperties) {
		return super.build(Map.of(
			Constant.USERNAME, mediaServerProperties.getUsername(),
			Constant.PASSWORD, mediaServerProperties.getPassword()
		));
	}
	
	@Override
	public void execute(Message message, WebSocket webSocket) {
		log.info("媒体终端注册结果：{}", message);
	}
	
	@Override
	public void execute(String sn, Client client, Message message) {
	}

}
