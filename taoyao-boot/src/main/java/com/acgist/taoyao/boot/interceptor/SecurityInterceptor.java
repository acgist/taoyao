package com.acgist.taoyao.boot.interceptor;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import com.acgist.taoyao.boot.config.SecurityProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 安全拦截
 * 
 * @author acgist
 */
@Slf4j
public class SecurityInterceptor implements HandlerInterceptor {

	/**
	 * 时间
	 */
	private ThreadLocal<Long> local = new ThreadLocal<>();
	
	@Autowired
	private SecurityProperties securityProperties;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(this.permit(request) || this.authorization(request)) {
			this.local.set(System.currentTimeMillis());
			return true;
		}
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic Realm=\"" + this.securityProperties.getRealm() + "\"");
		return false;
	}
	
	/**
	 * @param request 请求
	 * 
	 * @return 是否公共请求
	 */
	private boolean permit(HttpServletRequest request) {
		final String uri = request.getRequestURI();
		if(ArrayUtils.isEmpty(this.securityProperties.getPermit())) {
			return false;
		}
		for (String permit : this.securityProperties.getPermit()) {
			if(uri.startsWith(permit)) {
				log.debug("授权成功（公共请求）：{}-{}", uri, permit);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param request 请求
	 * 
	 * @return 是否授权成功
	 */
	private boolean authorization(HttpServletRequest request) {
		final String uri = request.getRequestURI();
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if(StringUtils.isEmpty(authorization)) {
			return false;
		}
		final int index = authorization.indexOf(' ');
		if(index < 0 || !authorization.substring(0, index).equalsIgnoreCase(SecurityProperties.BASIC)) {
			return false;
		}
		authorization = authorization.substring(index).strip();
		authorization = new String(Base64.getDecoder().decode(authorization));
		if(!authorization.equals(this.securityProperties.getAuthorization())) {
			return false;
		}
		log.debug("授权成功（Basic）：{}-{}", uri, authorization);
		return true;
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		final long duration;
		final Long last = this.local.get();
		if(last != null && (duration = System.currentTimeMillis() - last) > 1000) {
			log.info("执行时间过慢：{}-{}", request.getRequestURI(), duration);
		}
		this.local.remove();
	}
	
}
