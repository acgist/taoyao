package com.acgist.taoyao.signal.media;

/**
 * 终端媒体操作
 * 
 * @author acgist
 */
public interface ClientMediaHandler {

	/**
	 * 打开
	 */
	void open(String id);
	
	/**
	 * 暂停
	 */
	void pause();
	
	/**
	 * 恢复
	 */
	void resume();
	
	/**
	 * 关闭
	 */
	void close();
	
}
