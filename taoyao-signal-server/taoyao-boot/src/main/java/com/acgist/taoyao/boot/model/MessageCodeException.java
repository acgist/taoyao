package com.acgist.taoyao.boot.model;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

/**
 * 状态编码异常
 * 
 * @author acgist
 */
@Getter
public class MessageCodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 状态编码
     */
    private final MessageCode messageCode;

    /**
     * @param message 异常消息
     * 
     * @return 状态编码异常
     */
    public static final MessageCodeException of(String message) {
        return of(null, null, message);
    }
    
    /**
     * @param t       异常
     * @param message 异常消息
     * 
     * @return 状态编码异常
     */
    public static final MessageCodeException of(Throwable t, String message) {
        return of(t, null, message);
    }
    
    /**
     * @param messageCode 状态编码
     * @param message     异常消息
     * 
     * @return 状态编码异常
     */
    public static final MessageCodeException of(MessageCode messageCode, String message) {
        return of(null, messageCode, message);
    }
    
    /**
     * @param t           异常
     * @param messageCode 状态编码
     * @param message     异常消息
     * 
     * @return 状态编码异常
     */
    public static final MessageCodeException of(Throwable t, MessageCode messageCode, String message) {
        if(messageCode == null) {
            messageCode = MessageCode.CODE_9999;
        }
        if(StringUtils.isEmpty(message)) {
            message = Objects.isNull(t) ? messageCode.getMessage() : t.getMessage();
        }
        return new MessageCodeException(t, messageCode, message);
    }

    /**
     * @param t           异常
     * @param messageCode 状态编码
     * @param message     异常消息
     */
    public MessageCodeException(Throwable t, MessageCode messageCode, String message) {
        super(message, t);
        this.messageCode = messageCode;
    }

}
