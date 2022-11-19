package com.acgist.taoyao.interceptor;

import java.util.Base64;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.interceptor.InterceptorAdapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 安全拦截
 * 
 * @author acgist
 */
@Slf4j
public class SecurityInterceptor extends InterceptorAdapter {

	@Autowired
	private SecurityProperties securityProperties;
	
	@Override
	public String name() {
		return "安全拦截";
	}
	
	@Override
	public String[] pathPattern() {
		return new String[] { "/**" };
	}

	@Override
	public int getOrder() {
		return Integer.MIN_VALUE + 1;
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(this.permit(request) || this.authorization(request)) {
			return true;
		}
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic Realm=\"" + this.securityProperties.getRealm() + "\"");
		return false;
	}
	
	/**
	 * @param request 请求
	 * 
	 * @return 是否许可请求
	 */
	private boolean permit(HttpServletRequest request) {
		final String uri = request.getRequestURI();
		if(ArrayUtils.isEmpty(this.securityProperties.getPermit())) {
			return false;
		}
		for (String permit : this.securityProperties.getPermit()) {
			if(StringUtils.startsWith(uri, permit)) {
				log.debug("授权成功（许可请求）：{}-{}", uri, permit);
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
		if(!StringUtils.startsWithIgnoreCase(authorization, SecurityProperties.BASIC)) {
			return false;
		}
		authorization = authorization.substring(SecurityProperties.BASIC.length()).strip();
		authorization = new String(Base64.getDecoder().decode(authorization));
		if(!authorization.equals(this.securityProperties.getAuthorization())) {
			return false;
		}
		log.debug("授权成功（Basic）：{}-{}", uri, authorization);
		return true;
	}
	
}
