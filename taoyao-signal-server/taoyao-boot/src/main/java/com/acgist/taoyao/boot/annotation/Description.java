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
 * -[消息类型]> 异步请求 | 单播
 * =[消息类型]> 同步请求
 * -[消息类型]) 全员广播：对所有的终端广播信令（排除自己）
 * +[消息类型]) 全员广播：对所有的终端广播信令（包含自己）
 * 
 * 消息类型可以省略表示和前面一致
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
    String[] body() default "{}";
    
    /**
     * @return 数据流向
     */
    String[] flow() default "终端->信令服务->终端";

    /**
     * @return 详细描述
     */
    String memo() default "";

}
