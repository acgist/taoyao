package com.acgist.taoyao.signal.media.router;

import java.util.List;

import com.acgist.taoyao.signal.media.processor.ProcessorChain;
import com.acgist.taoyao.signal.media.stream.MediaStream;

/**
 * 媒体流路由器
 * 
 * @author acgist
 */
public interface MediaRouter {
	
	/**
	 * 初始路由
	 */
	void build();

	/**
	 * @return 媒体发布者
	 */
	MediaPublisher publisher();
	
	/**
	 * @return 媒体订阅者
	 */
	MediaSubscriber subscriber();
	
	/**
	 * @param processorChain 媒体流处理器责任链
	 */
	void processorChain(ProcessorChain processorChain);
	
	/**
	 * @return 发布者媒体流
	 */
	List<MediaStream> streamPublisher();
	
	/**
	 * @param sns 订阅者终端标识
	 * 
	 * @return 订阅者媒体流
	 */
	List<MediaStream> streamSubscriber(String ... sns);
	
	/**
	 * @param type 媒体类型
	 * 
	 * @return 发布者媒体流
	 */
	List<MediaStream> streamPublisher(MediaStream.Type type);
	
	/**
	 * @param type 媒体类型
	 * @param sns 订阅者终端标识
	 * 
	 * @return 发布者媒体流
	 */
	List<MediaStream> streamSubscriber(MediaStream.Type type, String ... sns);

	/**
	 * 关闭路由
	 */
	void close();

}
