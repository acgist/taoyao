package com.acgist.taoyao.signal.party.room;

import com.acgist.taoyao.signal.party.media.Kind;
import com.acgist.taoyao.signal.party.media.Producer;

/**
 * 媒体订阅类型
 * 
 * @author acgist
 */
public enum SubscribeType {
    
    /**
     * 订阅所有媒体
     */
    ALL,
    /**
     * 订阅所有音频媒体
     */
    ALL_AUDIO,
    /**
     * 订阅所有视频媒体
     */
    ALL_VIDEO,
    /**
     * 没有订阅任何媒体
     */
    NONE;
    
    /**
     * @param value 名称
     * 
     * @return 类型
     */
    public static final SubscribeType of(String value) {
        final SubscribeType[] values = SubscribeType.values();
        for (SubscribeType type : values) {
            if(type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return SubscribeType.ALL;
    }
    
    /**
     * @param producer 生产者
     * 
     * @return 是否可以消费
     */
    public boolean canConsume(Producer producer) {
        return switch (this) {
        case NONE      -> false;
        case ALL_AUDIO -> producer.getKind() == Kind.AUDIO;
        case ALL_VIDEO -> producer.getKind() == Kind.VIDEO;
        case ALL       -> true;
        default        -> true;
        };
    }
    
}
