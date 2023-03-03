package com.acgist.taoyao.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 信令描述
 * 
 * @author acgist
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Description {
    
    /**
     * @return 消息主体
     */
    String[] body() default { "" };
    
    /**
     * @return 数据流向
     */
    String[] flow() default { "终端=>信令服务->终端" };

    /**
     * @return 描述信息
     */
    String memo() default "";

}
