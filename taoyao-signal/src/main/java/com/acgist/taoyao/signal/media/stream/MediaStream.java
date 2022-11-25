package com.acgist.taoyao.signal.media.stream;

/**
 * 终端媒体流
 * 
 * @author acgist
 */
public interface MediaStream extends MediaHandler {

	/**
	 * 终端媒体类型
	 * 
	 * @author acgist
	 */
	public enum Type {
		
		/**
		 * 混合：音视频
		 */
		MIX,
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
	 * @return 终端媒体流类型
	 */
	Type type();
	
	/**
	 * @return 终端媒体流状态
	 */
	Status status();
	
	/**
	 * @return 发布者
	 */
	String publisher();
	
	/**
	 * @return 订阅者
	 */
	String subscriber();
	
}
