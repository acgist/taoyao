package com.acgist.taoyao.signal.party.media;

import com.acgist.taoyao.boot.model.MessageCodeException;

/**
 * 媒体类型
 * 
 * @author acgist
 */
public enum Kind {

    /**
     * 音频
     */
    AUDIO,
    /**
     * 视频
     */
    VIDEO;
    
    /**
     * @param value 类型
     * 
     * @return 类型
     */
    public static final Kind of(String value) {
        for (Kind kind : Kind.values()) {
            if(kind.name().equalsIgnoreCase(value)) {
                return kind;
            }
        }
        throw MessageCodeException.of("未知媒体类型：" + value);
    }
    
}
