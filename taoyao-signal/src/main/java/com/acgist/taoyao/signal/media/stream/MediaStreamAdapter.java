package com.acgist.taoyao.signal.media.stream;

/**
 * 终端媒体流适配器
 * 
 * @author acgist
 */
public abstract class MediaStreamAdapter<T> implements MediaStream {

	/**
	 * 媒体标识
	 */
	private String id;
	/**
	 * 真实流
	 */
	protected T stream;
	
}
