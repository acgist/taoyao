package com.acgist.taoyao.signal.media.stream;

import java.io.IOException;

/**
 * 终端媒体流
 * 
 * @author acgist
 */
public interface MediaStream {

	/**
	 * 终端媒体类型
	 * 
	 * @author acgist
	 */
	public enum Type {
		
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
	 * 终端媒体流状态
	 * 
	 * @author acgist
	 */
	public enum Status {
		
		/**
		 * 没有激活
		 */
		IDLE,
		/**
		 * 已经激活
		 */
		BUSY,
		/**
		 * 已经暂停
		 */
		PAUSE,
		/**
		 * 已经关闭
		 */
		CLOSE;
		
	}
	
	/**
	 * @return 终端媒体流ID
	 */
	String id();
	
	/**
	 * 打开终端媒体流
	 * 
	 * @throws IO异常
	 */
	void open() throws IOException;
	
	/**
	 * 暂停终端媒体流
	 * 
	 * @throws IO异常
	 */
	void pause() throws IOException;
	
	/**
	 * 恢复终端媒体流
	 * 
	 * @throws IO异常
	 */
	void resume() throws IOException;
	
	/**
	 * 关闭终端媒体流
	 * 
	 * @throws IO异常
	 */
	void close() throws IOException;
	
	/**
	 * @return 终端媒体流类型
	 */
	Type type();
	
	/**
	 * @return 终端媒体流状态
	 */
	Status status();
	
}
