package com.acgist.taoyao.mediasoup.protocol.client;

import java.net.http.WebSocket;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.MediasoupProperties;
import com.acgist.taoyao.boot.property.WebrtcProperties;
import com.acgist.taoyao.mediasoup.protocol.ProtocolMediasoupAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * Mediasoup终端授权信令
 * 
 * @author acgist
 */
@Slf4j
@Protocol
public class AuthorizeProtocol extends ProtocolMediasoupAdapter {

	public static final Integer PID = 6000;
	
	@Autowired
	private WebrtcProperties webrtcProperties;
	
	public AuthorizeProtocol() {
		super(PID, "Mediasoup终端授权信令");
	}
	
	@Override
	public Message build() {
		final MediasoupProperties mediasoup = this.webrtcProperties.getMediasoup();
		return super.build(Map.of(
			"username", mediasoup.getUsername(),
			"password", mediasoup.getPassword()
		));
	}
	
	@Override
	public void execute(Message message, WebSocket webSocket) {
		log.info("Mediasoup终端授权结果：{}", message);
	}

}
