package com.acgist.taoyao.signal.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.event.ApplicationEventAdapter;
import com.acgist.taoyao.signal.mediasoup.MediasoupClientManager;
import com.acgist.taoyao.signal.room.RoomManager;

/**
 * 事件监听适配器
 *
 * @param <E> 事件泛型
 * 
 * @author acgist
 */
public abstract class ApplicationListenerAdapter<E extends ApplicationEventAdapter> implements ApplicationListener<E> {

	@Autowired
	protected RoomManager roomManager;
	@Autowired
	protected ClientManager clientManager;
	@Autowired
	protected ApplicationContext applicationContext;
	@Autowired
	protected MediasoupClientManager mediasoupClientManager;
	
}
