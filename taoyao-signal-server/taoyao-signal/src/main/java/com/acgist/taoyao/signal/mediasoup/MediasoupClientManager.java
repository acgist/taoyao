package com.acgist.taoyao.signal.mediasoup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.property.WebrtcProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务终端管理
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
	 * 媒体服务终端列表
	 */
	private Map<String, MediasoupClient> clientMap = new ConcurrentHashMap<>();
	
	/**
	 * 加载媒体服务终端
	 */
	public void init() {
		this.webrtcProperties.getMediasoupList().stream()
		.filter(v -> Boolean.TRUE.equals(v.getEnabled()))
		.forEach(v -> {
			final MediasoupClient client = this.applicationContext.getBean(MediasoupClient.class);
			client.init(v);
			this.clientMap.put(client.name(), client);
			log.info("注册媒体服务终端：{}-{}", v.getAddress(), client);
		});
	}
	
	/**
	 * @param name 媒体服务终端名称
	 * 
	 * @return 媒体服务终端
	 */
	public MediasoupClient mediasoupClient(String name) {
		return this.clientMap.get(name);
	}
	
}
