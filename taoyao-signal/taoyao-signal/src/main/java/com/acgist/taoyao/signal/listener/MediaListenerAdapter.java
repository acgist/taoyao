package com.acgist.taoyao.signal.listener;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.signal.event.ApplicationEventAdapter;
import com.acgist.taoyao.signal.media.MediaRouterManager;

/**
 * 媒体事件监听适配器
 *
 * @param <E> 事件泛型
 * 
 * @author acgist
 */
public abstract class MediaListenerAdapter<E extends ApplicationEventAdapter> extends ApplicationListenerAdapter<E> {

	@Autowired
	protected MediaRouterManager mediaRouterManager;
	
}
