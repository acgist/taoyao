package com.acgist.taoyao.signal.media.router;

/**
 * 媒体流路由器
 * 
 * 发布者->订阅者
 * 
 * @author acgist
 */
public interface MediaRouter {

	void from();
	
	void to();
	
	void publisher();
	
	void subscriber();
	
	void stream(String fromOrTo);
	
	void streamFrom(String from);
	
	void streamTo(String to);
	
}
