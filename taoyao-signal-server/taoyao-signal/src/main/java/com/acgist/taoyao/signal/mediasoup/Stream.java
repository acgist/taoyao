package com.acgist.taoyao.signal.mediasoup;

import java.io.Closeable;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 终端媒体流
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Stream implements Closeable {

	/**
	 * 类型
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
		
	}
	
	/**
	 * 媒体流ID
	 */
	private String streamId;
	/**
	 * 媒体流描述
	 */
	private String description;
	
	@Override
	public void close() {
		log.info("关闭媒体：{}", this.streamId);
		// TODO：发送
	}
	
}
