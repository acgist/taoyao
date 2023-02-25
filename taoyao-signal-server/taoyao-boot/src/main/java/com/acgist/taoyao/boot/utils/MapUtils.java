package com.acgist.taoyao.boot.utils;

import java.util.Map;

/**
 * Map工具
 * 
 * @author acgist
 */
public final class MapUtils {

    private MapUtils() {
    }

    /**
     * @param <T> 参数泛型
     * 
     * @param body 消息主体
     * @param key 参数名称
     * 
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public static final <T> T get(Map<?, ?> body, String key) {
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
     * @param defaultValue 参数默认值
     * 
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public static final <T> T get(Map<?, ?> body, String key, T defaultValue) {
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
     * @return 参数值
     */
    public static final Long getLong(Map<?, ?> body, String key) {
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
     * @return 参数值
     */
    public static final Integer getInteger(Map<?, ?> body, String key) {
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
     * @return 参数值
     */
    public static final Boolean getBoolean(Map<?, ?> body, String key) {
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
