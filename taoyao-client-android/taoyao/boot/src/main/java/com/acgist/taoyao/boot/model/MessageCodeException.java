package com.acgist.taoyao.boot.model;

import java.util.Objects;

/**
 * 状态编码异常
 * 
 * @author acgist
 */
public class MessageCodeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 状态编码
	 */
	private final MessageCode code;

	/**
	 * @param message 异常消息
	 * 
	 * @return 状态编码异常
	 */
	public static final MessageCodeException of(String message) {
		return of(null, null, message);
	}
	
	/**
	 * @param t 异常
	 * @param message 异常消息
	 * 
	 * @return 状态编码异常
	 */
	public static final MessageCodeException of(Throwable t, String message) {
		return of(t, null, message);
	}
	
	/**
	 * @param code 状态编码
	 * @param message 异常消息
	 * 
	 * @return 状态编码异常
	 */
	public static final MessageCodeException of(MessageCode code, String message) {
		return of(null, code, message);
	}
	
	/**
	 * @param t 异常
	 * @param code 状态编码
	 * @param message 异常消息
	 * 
	 * @return 状态编码异常
	 */
	public static final MessageCodeException of(Throwable t, MessageCode code, String message) {
		if(code == null) {
			code = MessageCode.CODE_9999;
		}
		if(message == null || message.isEmpty()) {
			message = Objects.isNull(t) ? code.getMessage() : t.getMessage();
		}
		return new MessageCodeException(t, code, message);
	}

	/**
	 * @param t 异常
	 * @param code 状态编码
	 * @param message 异常消息
	 */
	public MessageCodeException(Throwable t, MessageCode code, String message) {
		super(message, t);
		this.code = code;
	}

}
