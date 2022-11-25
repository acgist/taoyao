package com.acgist.taoyao.signal.media.stream;

import java.io.IOException;

/**
 * 终端媒体处理器
 * 
 * @author acgist
 */
public interface MediaHandler {
	
	/**
	 * 打开
	 * 注意：用于打开媒体流
	 * 
	 * @throws IOException IO异常
	 */
	void open() throws IOException;

	/**
	 * 打开
	 * 注意：用于管理媒体流
	 * 
	 * @param stream 媒体流
	 * 
	 * @throws IOException IO异常
	 */
	void open(MediaStream stream) throws IOException;
	
	/**
	 * 暂停
	 * 注意：暂停时发送心跳防止通道关闭
	 * 
	 * @throws IOException IO异常
	 */
	void pause() throws IOException;
	
	/**
	 * 恢复
	 * 
	 * @throws IOException IO异常
	 */
	void resume() throws IOException;
	
	/**
	 * 关闭
	 * 
	 * @param id 终端媒体流ID
	 * 
	 * @throws IOException IO异常
	 */
	void close() throws IOException;
	
	/**
	 * 打开
	 * 
	 * @param type 媒体类型
	 * 
	 * @throws IOException IO异常
	 */
	void open(MediaStream.Type type) throws IOException;
	
	/**
	 * 暂停
	 * 注意：暂停时发送心跳防止通道关闭
	 * 
	 * @param type 媒体类型
	 * 
	 * @throws IOException IO异常
	 */
	void pause(MediaStream.Type type) throws IOException;
	
	/**
	 * 恢复
	 * 
	 * @param type 媒体类型
	 * 
	 * @throws IOException IO异常
	 */
	void resume(MediaStream.Type type) throws IOException;
	
	/**
	 * 关闭
	 * 
	 * @param type 媒体类型
	 * 
	 * @param id 终端媒体流ID
	 * 
	 * @throws IOException IO异常
	 */
	void close(MediaStream.Type type) throws IOException;
	
}
