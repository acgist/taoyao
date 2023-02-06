package com.acgist.taoyao.mediasoup;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.property.WebrtcProperties;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Mediasoup客户端管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class MediasoupClientManager {

	@Autowired
	private WebrtcProperties webrtcProperties;
	@Autowired
	private ApplicationContext applicationContext;
	
	/**
	 * Mediasoup客户端列表
	 */
	private List<MediasoupClient> clientList = new ArrayList<>();
	
	@PostConstruct
	public void init() {
		this.webrtcProperties.getMediasoupList().forEach(v -> {
			final MediasoupClient client = this.applicationContext.getBean(MediasoupClient.class);
			client.init(v);
			this.clientList.add(client);
			log.info("新建MediasoupClient：{}-{}", v.getAddress(), client);
		});
	}
	
}
