package com.acgist.taoyao.boot.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常处理工具
 * 
 * @author acgist
 */
@Slf4j
public final class ErrorUtils {
	
	/**
	 * 异常映射
	 */
	private static final Map<Class<?>, MessageCode> CODE_MAPPING = new LinkedHashMap<>();
	
	/**
	 * 错误地址
	 */
	public static final String ERROR_PATH = "/error";
	/**
	 * Servlet错误异常
	 */
	public static final String EXCEPTION_SERVLET = "javax.servlet.error.exception";
	/**
	 * Servlet错误编码
	 */
	public static final String SERVLET_STATUS_CODE = "javax.servlet.error.status_code";
	/**
	 * Servlet错误地址
	 */
	public static final String SERVLET_REQUEST_URI = "javax.servlet.error.request_uri";
	/**
	 * SpringBoot异常
	 */
	public static final String EXCEPTION_SPRINGBOOT = "org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR";

	private ErrorUtils() {
	}
	
	/**
	 * 注册异常（注意继承顺序）
	 * 
	 * @param code 异常编码
	 * @param clazz 异常类型
	 */
	public static final void register(MessageCode code, Class<?> clazz) {
		log.info("注册异常映射：{}-{}", code, clazz);
		CODE_MAPPING.put(clazz, code);
	}
	
	/**
	 * @param request 请求
	 * @param response 响应
	 * 
	 * @return 错误信息
	 */
	public static final Message message(HttpServletRequest request, HttpServletResponse response) {
		return message(null, request, response);
	}
	
	/**
	 * @param t 异常
	 * @param request 请求
	 * @param response 响应
	 * 
	 * @return 错误信息
	 */
	public static final Message message(Throwable t, HttpServletRequest request, HttpServletResponse response) {
		final Message message;
		int status = globalStatus(request, response);
		final Object globalError = t == null ? globalError(request) : t;
		final Object rootError = rootException(globalError);
		if(rootError instanceof MessageCodeException) {
			// 自定义的异常
			final MessageCodeException messageCodeException = (MessageCodeException) rootError;
			final MessageCode messageCode = messageCodeException.getCode();
			status = messageCode.getStatus();
			message = Message.fail(messageCode, messageCodeException.getMessage());
		} else if(rootError instanceof Throwable) {
			// 未知异常
			final Throwable throwable = (Throwable) rootError;
			final MessageCode messageCode = messageCode(status, throwable);
			status = messageCode.getStatus();
			message = Message.fail(messageCode, message(messageCode, throwable));
		} else {
			// 没有异常
			final MessageCode messageCode = MessageCode.of(status);
//			status = messageCode.getStatus();
			message = Message.fail(messageCode);
		}
		// 状态编码
		if(status == HttpServletResponse.SC_OK) {
			status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		response.setStatus(status);
		final String path = Objects.toString(request.getAttribute(SERVLET_REQUEST_URI), request.getServletPath());
		final String query = request.getQueryString();
		final String method = request.getMethod();
		if(globalError instanceof Throwable) {
			log.error("""
				请求错误
				请求地址：{}
				请求参数：{}
				请求方法：{}
				错误信息：{}
				响应状态：{}
				""", path, query, method, message, status, globalError);
		} else {
			log.warn("""
				请求错误
				请求地址：{}
				请求参数：{}
				请求方法：{}
				错误信息：{}
				响应状态：{}
				原始信息：{}
				""", path, query, method, message, status, globalError);
		}
		return message;
	}
	
	/**
	 * @param request 请求
	 * @param response 响应
	 * 
	 * @return 响应状态
	 */
	public static final int globalStatus(HttpServletRequest request, HttpServletResponse response) {
		final Object status = request.getAttribute(SERVLET_STATUS_CODE);
		if(status instanceof Integer) {
			return (Integer) status;
		}
		return response.getStatus();
	}
	
	/**
	 * @param request 请求
	 * 
	 * @return 异常
	 */
	public static final Object globalError(HttpServletRequest request) {
		// Servlet错误异常
		Object throwable = request.getAttribute(EXCEPTION_SERVLET);
		if(throwable != null) {
			return throwable;
		}
		// SpringBoot异常
		throwable = request.getAttribute(EXCEPTION_SPRINGBOOT);
		if(throwable != null) {
			return throwable;
		}
		return throwable;
	}
	
	/**
	 * @param status 原始状态
	 * @param t 异常
	 * 
	 * @return 响应状态
	 * 
	 * @see ResponseEntityExceptionHandler
	 * @see DefaultHandlerExceptionResolver
	 */
	public static final MessageCode messageCode(int status, Throwable t) {
		return CODE_MAPPING.entrySet().stream()
			.filter(entry -> {
				final Class<?> clazz = t.getClass();
				final Class<?> mappingClazz = entry.getKey();
				return mappingClazz.equals(clazz) || mappingClazz.isAssignableFrom(clazz);
			})
			.map(Map.Entry::getValue)
			.findFirst()
			.orElse(MessageCode.of(status));
	}
	
	/**
	 * @param messageCode 错误编码
	 * @param t 异常
	 * 
	 * @return 异常信息
	 */
	public static final String message(MessageCode messageCode, Throwable t) {
		// ValidationException
		if(
			t instanceof BindException ||
			t instanceof MethodArgumentNotValidException
		) {
			final BindException bindException = (BindException) t;
			final List<ObjectError> allErrors = bindException.getAllErrors();
			return allErrors.stream().map(ObjectError::getDefaultMessage).collect(Collectors.joining(","));
		}
		// 为了系统安全建议不要直接返回
		final String message = t.getMessage();
		if(messageCode == MessageCode.CODE_9999 && StringUtils.isNotEmpty(message)) {
			return message;
		}
		if(StringUtils.isNotEmpty(message) && message.length() < 64) {
			return message;
		}
		return messageCode.getMessage();
	}
	
	/**
	 * @param t 异常
	 * 
	 * @return 原始异常
	 * 
	 * @see #rootException(Throwable)
	 */
	public static final Object rootException(Object t) {
		if(t instanceof Throwable) {
			return rootException((Throwable) t);
		}
		return t;
	}

	/**
	 * @param t 异常
	 * 
	 * @return 原始异常
	 */
	public static final Throwable rootException(Throwable t) {
		Throwable cause = t;
		do {
			// 直接返回状态编码异常
			if(cause instanceof MessageCodeException) {
				return cause;
			}
		} while(cause != null && (cause = cause.getCause()) != null);
		// 返回原始异常
		return t;
	}
	
}
