package com.acgist.taoyao.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

/**
 * 测试启动
 * 
 * @author acgist
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestExecutionListeners(listeners = CostedTestTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public @interface TaoyaoTest {

    @AliasFor(annotation = SpringBootTest.class)
    Class<?>[] classes() default {};

}
