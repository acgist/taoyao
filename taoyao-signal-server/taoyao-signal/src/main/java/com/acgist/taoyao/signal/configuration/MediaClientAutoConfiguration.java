package com.acgist.taoyao.signal.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import com.acgist.taoyao.signal.media.MediaClientManager;

/**
 * 媒体服务自动配置
 * 
 * @author acgist
 */
public class MediaClientAutoConfiguration {

    @Bean
    @Autowired
    public CommandLineRunner mediaCommandLineRunner(MediaClientManager mediaClientManager) {
        return new CommandLineRunner() {
            @Override
            public void run(String ... args) throws Exception {
                mediaClientManager.init();
            }
        };
    }
    
}
