package com.acgist.taoyao.interceptor;

import java.util.Base64;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;

import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.interceptor.InterceptorAdapter;
import com.acgist.taoyao.signal.service.SecurityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 安全拦截器
 * 
 * @author acgist
 */
@Slf4j
public class SecurityInterceptor extends InterceptorAdapter {

	private final SecurityService securityService;
	private final SecurityProperties securityProperties;
	
	public SecurityInterceptor(SecurityService securityService, SecurityProperties securityProperties) {
        this.securityService = securityService;
        this.securityProperties = securityProperties;
        log.info("安全拦截密码：{}", securityProperties.getPassword());
    }

    /**
	 * 地址匹配
	 */
	private final AntPathMatcher matcher = new AntPathMatcher();
	
	@Override
	public String name() {
		return "安全拦截器";
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
		if(log.isDebugEnabled()) {
			log.debug("授权失败：{}", request.getRequestURL());
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
		final String[] permit = this.securityProperties.getPermit();
		if(ArrayUtils.isEmpty(permit)) {
			return false;
		}
		for (String pattern : permit) {
			if(this.matcher.match(pattern, uri)) {
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
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if(StringUtils.isEmpty(authorization)) {
			return false;
		}
		int index = authorization.indexOf(' ');
		if(index < 0) {
		    return false;
		}
		authorization = authorization.substring(index + 1).strip();
		authorization = new String(Base64.getMimeDecoder().decode(authorization));
		index = authorization.indexOf(':');
		if(index < 0) {
			return false;
		}
		final String username = authorization.substring(0, index);
		final String password = authorization.substring(index + 1);
		return this.securityService.authenticate(username, password);
	}
	
}
