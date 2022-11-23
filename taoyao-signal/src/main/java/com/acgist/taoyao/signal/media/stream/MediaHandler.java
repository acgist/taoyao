package com.acgist.taoyao.signal.media.stream;

/**
 * 终端媒体操作
 * 
 * TODO：注意暂停心跳防止端口关闭
 * 
 * @author acgist
 */
public interface MediaHandler {

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
