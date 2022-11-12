package com.acgist.taoyao.signal.media;

/**
 * 终端媒体操作
 * 
 * @author acgist
 */
public interface ClientMediaHandler {

	/**
	 * 打开
	 * 
	 * @param id 终端媒体流ID
	 */
	void open(String id);
	
	/**
	 * 暂停
	 * 
	 * @param id 终端媒体流ID
	 */
	void pause(String id);
	
	/**
	 * 恢复
	 * 
	 * @param id 终端媒体流ID
	 */
	void resume(String id);
	
	/**
	 * 关闭
	 * 
	 * @param id 终端媒体流ID
	 */
	void close(String id);
	
}
