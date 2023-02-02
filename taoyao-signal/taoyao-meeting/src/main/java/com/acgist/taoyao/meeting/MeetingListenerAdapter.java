package com.acgist.taoyao.meeting;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.signal.event.ApplicationEventAdapter;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

/**
 * 会议事件监听适配器
 *
 * @param <E> 事件泛型
 * 
 * @author acgist
 */
public abstract class MeetingListenerAdapter<E extends ApplicationEventAdapter> extends ApplicationListenerAdapter<E> {

	@Autowired
	protected MeetingManager meetingManager;
	
}
