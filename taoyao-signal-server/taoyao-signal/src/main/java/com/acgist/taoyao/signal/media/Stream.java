package com.acgist.taoyao.signal.media;

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
		
	}
	
	/**
	 * 收发类型
	 * 
	 * @author acgist
	 */
	public enum Type {
		
		/**
		 * 发送
		 */
		SEND,
		/**
		 * 接收
		 */
		RECV;
		
	}
	
	/**
	 * 媒体终端标识
	 */
	private String sn;
	/**
	 * 媒体流ID
	 * 
	 * 媒体类型.发送终端标识.发送.房间ID：
	 * 音频：audio.sn.send.1000
	 * 视频：video.sn.send.1000
	 * 
	 * 媒体类型.接收终端标识.接收.发送终端标识.房间ID：
	 * 音频：audio.sn.recv.sn.1000
	 * 视频：video.sn.recv.sn.1000
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
