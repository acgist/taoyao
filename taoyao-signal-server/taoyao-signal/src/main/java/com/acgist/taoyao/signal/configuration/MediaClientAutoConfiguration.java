package com.acgist.taoyao.signal.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.boot.runner.OrderedCommandLineRunner;
import com.acgist.taoyao.signal.media.MediaClientManager;

/**
 * 媒体服务自动配置
 * 
 * @author acgist
 */
@Configuration
public class MediaClientAutoConfiguration {

    @Bean
    @Autowired
    public CommandLineRunner mediaCommandLineRunner(MediaClientManager mediaClientManager) {
        return new OrderedCommandLineRunner() {
            @Override
            public void run(String ... args) throws Exception {
                mediaClientManager.init();
            }
        };
    }
    
}
