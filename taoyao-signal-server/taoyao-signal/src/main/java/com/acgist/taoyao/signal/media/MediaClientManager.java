package com.acgist.taoyao.signal.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.config.MediaProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务终端管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class MediaClientManager {

	@Autowired
	private MediaProperties mediaProperties;
	@Autowired
	private ApplicationContext applicationContext;
	
	/**
	 * 媒体服务终端列表
	 */
	private Map<String, MediaClient> clientMap = new ConcurrentHashMap<>();
	
    @Scheduled(cron = "${taoyao.scheduled.media:0 * * * * ?}")
    public void scheduled() {
        this.heartbeat();
    }
	
	/**
	 * 加载媒体服务终端
	 */
	public void init() {
		this.mediaProperties.getMediaServerList().stream()
		.filter(v -> Boolean.TRUE.equals(v.getEnabled()))
		.forEach(v -> {
			final MediaClient client = this.applicationContext.getBean(MediaClient.class);
			client.init(v);
			this.clientMap.put(client.mediaId(), client);
			log.info("注册媒体服务终端：{}-{}", v.getMediaId(), v.getAddress());
		});
	}
	
	/**
	 * @param name 媒体服务终端名称
	 * 
	 * @return 媒体服务终端
	 */
	public MediaClient mediaClient(String name) {
		return this.clientMap.get(name);
	}

	/**
	 * 心跳
	 */
	private void heartbeat() {
	    this.clientMap.forEach((k, v) -> {
	        v.heartbeat();
	    });
	}
	
}
