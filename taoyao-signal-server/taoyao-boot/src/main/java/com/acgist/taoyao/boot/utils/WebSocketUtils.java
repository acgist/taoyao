package com.acgist.taoyao.boot.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket工具
 * 
 * @author acgist
 */
@Slf4j
public class WebSocketUtils {
	
	private WebSocketUtils() {
	}

	/**
	 * @param session WebSocket
	 * 
	 * @return 远程地址
	 */
	public static final String getRemoteAddress(Session session) {
		if (session == null) {
			return null;
		}
		// 远程IP地址
		return (String) getField(session.getAsyncRemote(), "base.socketWrapper.remoteAddr");
	}

	/**
	 * @param object 对象
	 * @param fieldPath 属性路径
	 * 
	 * @return 属性
	 */
	private static final Object getField(Object object, String fieldPath) {
		final String fields[] = StringUtils.split(fieldPath, '.');
		for (String field : fields) {
			object = getField(object, object.getClass(), field);
			if (object == null) {
				return null;
			}
		}
		return object;
	}

	/**
	 * @param object 对象
	 * @param clazz 属性类型
	 * @param fieldName 属性名称
	 * 
	 * @return 属性
	 */
	private static final Object getField(Object object, Class<?> clazz, String fieldName) {
		try {
			return FieldUtils.getField(clazz, fieldName, true).get(object);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.error("读取属性异常：{}-{}", clazz, fieldName, e);
		}
		return null;
	}

}
