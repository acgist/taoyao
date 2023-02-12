package com.acgist.taoyao.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 多线程测试
 * 
 * @author acgist
 */
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CostedTest {

	/**
	 * @return 执行次数
	 */
	int count() default 1;

	/**
	 * @return 线程数量
	 */
	int thread() default 1;
	
	/**
	 * @return 超时时间
	 */
	long timeout() default 1000L;
	
	/**
	 * @return 等待资源释放时间
	 */
	long waitRelease() default 0L;
	
	/**
	 * @return 超时时间单位
	 */
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
	
}
