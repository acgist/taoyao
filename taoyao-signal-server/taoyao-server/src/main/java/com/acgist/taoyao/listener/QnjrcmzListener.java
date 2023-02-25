package com.acgist.taoyao.listener;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;

import lombok.extern.slf4j.Slf4j;

/**
 * 去年今日此门中
 * 人面桃花相映红
 * 
 * 注意：SpringApplicationRunListeners里面同步执行
 * 
 * @author acgist
 */
@Slf4j
public class QnjrcmzListener implements ApplicationListener<ApplicationStartingEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartingEvent event) {
        synchronized (QnjrcmzListener.class) {
            log.debug("配置忽略证书域名校验");
            // 配置JDK HTTPClient域名校验问题
            System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
        }
    }

}
