package com.acgist.taoyao.signal.media.router;

/**
 * 直播会议路由绑定
 * 
 * 发布者->订阅者
 * 
 * @author acgist
 */
public interface Router {

	void from();
	
	void to();
	
}
