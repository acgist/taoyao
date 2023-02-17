package com.acgist.taoyao.signal.event;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.media.MediaClient;

import lombok.Getter;

/**
 * 媒体事件适配器
 * 
 * @author acgist
 */
@Getter
public class MediaEventAdapter extends ApplicationEventAdapter {

    private static final long serialVersionUID = 1L;

    /**
     * 媒体服务终端
     */
    private final MediaClient mediaClient;
    
    public MediaEventAdapter(Message message, MediaClient mediaClient) {
        this(Map.of(), message, mediaClient);
    }
    
    public MediaEventAdapter(Map<?, ?> body, Message message, MediaClient mediaClient) {
        super(body, message, mediaClient);
        this.mediaClient = mediaClient;
    }

}
