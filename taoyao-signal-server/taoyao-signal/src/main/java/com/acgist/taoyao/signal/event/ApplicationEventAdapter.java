package com.acgist.taoyao.signal.event;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;

import lombok.Getter;

/**
 * 事件适配器
 * 
 * @author acgist
 */
@Getter
public class ApplicationEventAdapter extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 信令消息
     */
    private final Message message;
    /**
     * 信令主体
     */
    private final Map<String, Object> body;
    
    protected ApplicationEventAdapter(Object source, Message message, Map<String, Object> body) {
        super(source);
        this.body    = body;
        this.message = message;
    }
    
    /**
     * @see MapUtils#get(Map, String)
     */
    public <T> T get(String key) {
        return MapUtils.get(this.body, key);
    }

    /**
     * @see MapUtils#get(Map, String, Object)
     */
    public <T> T get(String key, T defaultValue) {
        return MapUtils.get(this.body, key, defaultValue);
    }
    
    /**
     * @see MapUtils#getLong(Map, String)
     */
    public Long getLong(String key) {
        return MapUtils.getLong(this.body, key);
    }
    
    /**
     * @see MapUtils#getDouble(Map, String)
     */
    public Double getDouble(String key) {
        return MapUtils.getDouble(this.body, key);
    }
    
    /**
     * @see MapUtils#getInteger(Map, String)
     */
    public Integer getInteger(String key) {
        return MapUtils.getInteger(this.body, key);
    }
    
    /**
     * @see MapUtils#getBoolean(Map, String)
     */
    public Boolean getBoolean(String key) {
        return MapUtils.getBoolean(this.body, key);
    }

}
