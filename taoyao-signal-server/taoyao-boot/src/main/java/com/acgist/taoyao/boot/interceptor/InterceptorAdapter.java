package com.acgist.taoyao.boot.interceptor;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * MVC拦截器适配器
 * 
 * @author acgist
 */
public abstract class InterceptorAdapter implements Ordered, Comparable<InterceptorAdapter>, HandlerInterceptor {

    /**
     * @return 名称
     */
    public abstract String name();
    
    /**
     * @return 拦截地址
     */
    public abstract String[] pathPattern();
    
    @Override
    public int compareTo(InterceptorAdapter target) {
        return Integer.compare(this.getOrder(), target.getOrder());
    }
    
}
