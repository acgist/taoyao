package com.acgist.taoyao.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

import com.acgist.taoyao.boot.annotation.Listener;
import com.acgist.taoyao.boot.config.TaoyaoProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 人面不知何处去
 * 桃花依旧笑春风
 * 
 * @author acgist
 */
@Slf4j
@Listener
public class ThyjxcfListener implements ApplicationListener<ApplicationReadyEvent> {
    
    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        synchronized (ThyjxcfListener.class) {
            final TaoyaoProperties taoyaoProperties = event.getApplicationContext().getBean(TaoyaoProperties.class);
            log.info("项目启动成功：{}", taoyaoProperties.getName());
        }
    }

}
