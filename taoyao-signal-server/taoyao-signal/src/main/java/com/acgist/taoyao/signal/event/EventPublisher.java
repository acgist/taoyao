package com.acgist.taoyao.signal.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * 事件发布者
 * 
 * @author acgist
 */
public class EventPublisher {

    /**
     * 上下文
     */
    private static ApplicationContext applicationContext;

    /**
     * @param applicationContext 上下文
     */
    public static final void setApplicationContext(ApplicationContext applicationContext) {
        EventPublisher.applicationContext = applicationContext;
    }
    
    /**
     * 发布事件
     * 
     * @param event 事件
     */
    public static final void publishEvent(ApplicationEvent event) {
        EventPublisher.applicationContext.publishEvent(event);
    }
    
}
