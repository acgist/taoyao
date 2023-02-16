package com.acgist.taoyao.signal.protocol;

import java.util.Map;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.media.MediaClient;

/**
 * 媒体服务信令协议适配器
 * 
 * @author acgist
 */
public abstract class ProtocolMediaAdapter extends ProtocolClientAdapter {

	protected ProtocolMediaAdapter(String name, String signal) {
		super(name, signal);
	}
	
	/**
	 * 处理媒体服务信令
	 * 
	 * @param mediaClient 媒体服务终端
	 * @param message 信令消息
	 */
	public void execute(MediaClient mediaClient, Message message) {
        final Object body = message.getBody();
        if(body instanceof Map<?, ?> map) {
            this.execute(map, mediaClient, message);
        } else if(body == null) {
            this.execute(Map.of(), mediaClient, message);
        } else {
            throw MessageCodeException.of("信令主体类型错误：" + message);
        }
    }
	
	/**
	 * 处理媒体服务信令
	 * 
	 * @param body 信令主体
	 * @param mediaClient 媒体服务终端
	 * @param message 信令消息
	 */
    public abstract void execute(Map<?, ?> body, MediaClient mediaClient, Message message);
	
}
