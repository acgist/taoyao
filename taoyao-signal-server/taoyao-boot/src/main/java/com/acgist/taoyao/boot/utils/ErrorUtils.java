package com.acgist.taoyao.boot.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常处理工具
 * 
 * @author acgist
 */
@Slf4j
public final class ErrorUtils {
    
    private ErrorUtils() {
    }
    
    /**
     * 异常映射
     */
    private static final Map<Class<?>, MessageCode> CODE_MAPPING = new LinkedHashMap<>();
    
    /**
     * 错误地址
     */
    public static final String ERROR_PATH = "/error";
    /**
     * Servlet错误编码
     */
    public static final String SERVLET_STATUS_CODE = "javax.servlet.error.status_code";
    /**
     * Servlet错误地址
     */
    public static final String SERVLET_REQUEST_URI = "javax.servlet.error.request_uri";
    /**
     * Servlet错误异常
     */
    public static final String EXCEPTION_SERVLET = "javax.servlet.error.exception";
    /**
     * SpringBoot异常
     */
    public static final String EXCEPTION_SPRINGBOOT = "org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR";
    
    /**
     * 注册异常（注意继承顺序）
     * 
     * @param messageCode 状态编码
     * @param clazz       异常类型
     */
    public static final void register(MessageCode messageCode, Class<?> clazz) {
        log.debug("注册状态编码异常映射：{} - {}", messageCode, clazz);
        synchronized (CODE_MAPPING) {
            CODE_MAPPING.put(clazz, messageCode);
        }
    }
    
    /**
     * @param request  请求
     * @param response 响应
     * 
     * @return 错误消息
     */
    public static final Message message(HttpServletRequest request, HttpServletResponse response) {
        return ErrorUtils.message(null, request, response);
    }
    
    /**
     * @param t        异常
     * @param request  请求
     * @param response 响应
     * 
     * @return 错误消息
     */
    public static final Message message(Throwable t, HttpServletRequest request, HttpServletResponse response) {
        final Message message;
        // 状态编码
        int status = ErrorUtils.globalStatus(request, response);
        // 全局异常
        final Object globalError = t == null ? ErrorUtils.globalError(request) : t;
        // 原始异常
        final Object rootError = ErrorUtils.rootException(globalError);
        if(rootError instanceof MessageCodeException messageCodeException) {
            // 状态编码异常
            final MessageCode messageCode = messageCodeException.getMessageCode();
            status  = messageCode.getStatus();
            message = Message.fail(messageCode, messageCodeException.getMessage());
        } else if(rootError instanceof Throwable throwable) {
            // 未知异常：异常转换
            final MessageCode messageCode = ErrorUtils.messageCode(status, throwable);
            status  = messageCode.getStatus();
            message = Message.fail(messageCode, ErrorUtils.message(messageCode, throwable));
        } else {
            // 没有异常
            final MessageCode messageCode = MessageCode.of(status);
            message = Message.fail(messageCode);
        }
        // 状态编码
        if(status == HttpServletResponse.SC_OK) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        response.setStatus(status);
        // 请求地址
        final String path = ErrorUtils.getFirstParams(
            request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI),
            request.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH),
            request.getAttribute(SERVLET_REQUEST_URI),
            request.getServletPath()
        );
        // 请求参数
        final String query = ErrorUtils.getFirstParams(
            request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING),
            request.getQueryString()
        );
        // 请求方法
        final String method = request.getMethod();
        if(globalError instanceof Throwable) {
            log.error("""
                请求异常
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
        final Map<String, String> body = new HashMap<>();
        body.put("path",   path);
        body.put("query",  query);
        body.put("method", method);
        message.setBody(body);
        return message;
    }
    
    /**
     * @param request  请求
     * @param response 响应
     * 
     * @return 状态编码
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
     * @param status    原始状态编码
     * @param throwable 异常
     * 
     * @return 状态编码
     * 
     * @see ResponseEntityExceptionHandler
     * @see DefaultHandlerExceptionResolver
     */
    public static final MessageCode messageCode(int status, Throwable throwable) {
        final Class<?> clazz = throwable.getClass();
        return CODE_MAPPING.entrySet().stream()
            .filter(entry -> {
                final Class<?> mappingClazz = entry.getKey();
                return mappingClazz.equals(clazz) || mappingClazz.isAssignableFrom(clazz);
            })
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(MessageCode.of(status));
    }
    
    /**
     * @param messageCode 状态编码
     * @param throwable   异常
     * 
     * @return 异常信息
     */
    public static final String message(MessageCode messageCode, Throwable throwable) {
        // 校验异常
        if(throwable instanceof BindException bindException) {
            return bindException.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(" && "));
        }
        // 校验异常
        if(throwable instanceof ConstraintViolationException violationException) {
            return violationException.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(" && "));
        }
        final String message = throwable.getMessage();
        // 自定义的系统异常
        if(StringUtils.isNotEmpty(message) && messageCode != MessageCode.CODE_9999) {
            return message;
        }
        // 不要直接返回异常堆栈信息
//      if(StringUtils.isNotEmpty(message)) {
//          return message;
//      }
        // 其他情况不能直接返回异常信息
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
        if(t instanceof Throwable throwable) {
            return ErrorUtils.rootException(throwable);
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
    
    /**
     * @param params 参数列表
     * 
     * @return 首个参数
     */
    private static final String getFirstParams(Object ... params) {
        for (final Object object : params) {
            if(object != null) {
                return object.toString();
            }
        }
        return null;
    }
    
}
