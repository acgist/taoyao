package com.acgist.taoyao.boot.utils;

import java.math.BigDecimal;
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
        } else if(object instanceof Integer value) {
            return value.longValue();
        } else if(object instanceof Double value) {
            return value.longValue();
        }
        return new BigDecimal(object.toString()).longValue();
    }
    
    /**
     * @param body 消息主体
     * @param key 参数名称
     * 
     * @return 参数值
     */
    public static final Double getDouble(Map<?, ?> body, String key) {
        if(body == null) {
            return null;
        }
        final Object object = body.get(key);
        if(object == null) {
            return null;
        } else if(object instanceof Long value) {
            return value.doubleValue();
        } else if(object instanceof Integer value) {
            return value.doubleValue();
        } else if(object instanceof Double value) {
            return value;
        }
        return new BigDecimal(object.toString()).doubleValue();
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
        } else if(object instanceof Long value) {
            return value.intValue();
        } else if(object instanceof Integer value) {
            return value;
        } else if(object instanceof Double value) {
            return value.intValue();
        }
        return new BigDecimal(object.toString()).intValue();
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

    /**
     * @param <T> 参数泛型
     * 
     * @param body 消息主体
     * @param key 参数名称
     * 
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public static final <T> T remove(Map<?, ?> body, String key) {
        if(body == null) {
            return null;
        }
        return (T) body.remove(key);
    }
    
}
