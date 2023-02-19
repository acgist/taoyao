package com.acgist.taoyao.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * 信令描述
 * 生成文档采用`RUNTIME`
 * 
 * @author acgist
 */
@Target(ElementType.TYPE)
@Component
//@Retention(RetentionPolicy.SOURCE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Description {
    
    /**
     * @return 消息主体
     */
    String[] body() default { "{}" };
    
    /**
     * @return 数据流向
     */
    String[] flow() default { "终端->信令服务->终端" };

    /**
     * @return 描述信息
     */
    String memo() default "";

}
