package com.acgist.taoyao.signal.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import com.acgist.taoyao.signal.client.ClientSessionManager;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;

/**
 * 事件监听适配器
 *
 * @param <E> 事件泛型
 * 
 * @author acgist
 */
public abstract class ApplicationListenerAdapter<E extends ApplicationEventAdapter> implements ApplicationListener<E> {

	@Autowired
	protected ClientSessionManager clientSessionManager;
	
}
