package com.acgist.taoyao.interceptor;

import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.interceptor.InterceptorAdapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 过慢请求拦截器
 * 
 * @author acgist
 */
@Slf4j
public class SlowInterceptor extends InterceptorAdapter {
	
	private final TaoyaoProperties taoyaoProperties;
	
	/**
	 * 请求开始时间
	 */
	private final ThreadLocal<Long> local;
	
	public SlowInterceptor(TaoyaoProperties taoyaoProperties) {
        this.taoyaoProperties = taoyaoProperties;
        this.local            = new ThreadLocal<>();
    }
	
	@Override
	public String name() {
		return "过慢请求拦截器";
	}
	
	@Override
	public String[] pathPattern() {
		return new String[] { "/**" };
	}
	
	@Override
	public int getOrder() {
		return Integer.MIN_VALUE;
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		this.local.set(System.currentTimeMillis());
		return true;
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) throws Exception {
		final long duration;
		final Long last = this.local.get();
		if(last != null && (duration = System.currentTimeMillis() - last) > this.taoyaoProperties.getTimeout()) {
			log.info("请求执行时间过慢：{} - {}", request.getRequestURI(), duration);
		}
		this.local.remove();
	}
	
}
