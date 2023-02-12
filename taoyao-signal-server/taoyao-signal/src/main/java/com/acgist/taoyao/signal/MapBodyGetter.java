package com.acgist.taoyao.signal;

import java.util.Map;

/**
 * Map参数
 * 
 * @author acgist
 */
public interface MapBodyGetter {

	/**
	 * @param <T> 参数泛型
	 * 
	 * @param body 消息主体
	 * @param key 参数名称
	 * 
	 * @return 值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(Map<?, ?> body, String key) {
		if(body == null) {
			return null;
		}
		return (T) body.get(key);
	}

	/**
	 * @param <T> 参数泛型
	 * 
	 * @param body 消息主体
	 * @param key 参数名称
	 * @param defaultValue 默认值
	 * 
	 * @return 值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(Map<?, ?> body, String key, T defaultValue) {
		if(body == null) {
			return defaultValue;
		}
		final T t = (T) body.get(key);
		return t == null ? defaultValue : t;
	}
	
	
	/**
	 * @param body 消息主体
	 * @param key 参数名称
	 * 
	 * @return 值
	 */
	default Long getLong(Map<?, ?> body, String key) {
		if(body == null) {
			return null;
		}
		final Object object = body.get(key);
		if(object == null) {
			return null;
		} else if(object instanceof Long value) {
			return value;
		}
		return Long.valueOf(object.toString());
	}
	
	/**
	 * @param body 消息主体
	 * @param key 参数名称
	 * 
	 * @return 值
	 */
	default Integer getInteger(Map<?, ?> body, String key) {
		if(body == null) {
			return null;
		}
		final Object object = body.get(key);
		if(object == null) {
			return null;
		} else if(object instanceof Integer value) {
			return value;
		}
		return Integer.valueOf(object.toString());
	}
	
	/**
	 * @param body 消息主体
	 * @param key 参数名称
	 * 
	 * @return 值
	 */
	default Boolean getBoolean(Map<?, ?> body, String key) {
		if(body == null) {
			return null;
		}
		final Object object = body.get(key);
		if(object == null) {
			return null;
		} else if(object instanceof Boolean value) {
			return value;
		}
		return Boolean.valueOf(object.toString());
	}
	
}
