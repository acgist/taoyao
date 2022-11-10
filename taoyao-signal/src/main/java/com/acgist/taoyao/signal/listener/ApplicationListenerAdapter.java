package com.acgist.taoyao.signal.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.acgist.taoyao.signal.session.ClientSessionManager;

/**
 * 事件监听
 *
 * @param <E> 事件泛型
 * 
 * @author acgist
 */
public abstract class ApplicationListenerAdapter<E extends ApplicationEvent> implements ApplicationListener<E> {

	@Autowired
	protected ClientSessionManager clientSessionManager;
	
}
