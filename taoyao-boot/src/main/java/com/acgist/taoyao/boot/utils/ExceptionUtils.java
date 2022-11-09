package com.acgist.taoyao.boot.utils;

import com.acgist.taoyao.boot.model.MessageCodeException;

/**
 * 异常工具
 * 
 * @author acgist
 */
public class ExceptionUtils {
	
	/**
	 * @param t 异常
	 * 
	 * @return 原始异常
	 * 
	 * @see #root(Throwable)
	 */
	public static final Object root(Object t) {
		if(t instanceof Throwable) {
			return ExceptionUtils.root((Throwable) t);
		}
		return t;
	}

	/**
	 * @param t 异常
	 * 
	 * @return 原始异常
	 */
	public static final Throwable root(Throwable t) {
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
