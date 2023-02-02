package com.acgist.taoyao.signal.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.signal.media.processor.ProcessorChain;
import com.acgist.taoyao.signal.media.router.MediaRouter;
import com.acgist.taoyao.signal.media.router.MediaRouterHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体路由管理
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class MediaRouterManager {
	
	/**
	 * 路由集合
	 * ID=路由器
	 * ID=LiveId/MeetingId
	 */
	private Map<Long, MediaRouter> routers = new ConcurrentHashMap<>();
	
	@Autowired(required = false)
	private ProcessorChain processorChain;
	
	/**
	 * 创建路由
	 * 
	 * @param id ID
	 * 
	 * @return 路由
	 */
	public MediaRouter build(Long id) {
		return this.routers.computeIfAbsent(id, key -> {
			final MediaRouter router = new MediaRouterHandler();
			router.build();
			router.processorChain(this.processorChain);
			log.debug("创建路由：{}-{}", id, router);
			return router;
		});
	}
	
	/**
	 * @param id ID
	 * 
	 * @return 路由
	 */
	public MediaRouter router(Long id) {
		return this.routers.get(id);
	}
	
	/**
	 * 关闭路由
	 * 
	 * @param id ID
	 */
	public void close(Long id) {
		final MediaRouter router = this.router(id);
		if(router == null) {
			return;
		}
		router.close();
	}
	
}
