package com.acgist.taoyao.interceptor;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.interceptor.InterceptorAdapter;
import com.acgist.taoyao.boot.property.TaoyaoProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 过慢请求统计拦截
 * 
 * @author acgist
 */
@Slf4j
public class SlowInterceptor extends InterceptorAdapter {

	/**
	 * 时间
	 */
	private ThreadLocal<Long> local = new ThreadLocal<>();
	
	@Autowired
	private TaoyaoProperties taoyaoProperties;
	
	@Override
	public String name() {
		return "过慢请求统计拦截";
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
			log.info("请求执行时间过慢：{}-{}", request.getRequestURI(), duration);
		}
		this.local.remove();
	}
	
}
