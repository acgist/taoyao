package com.acgist.taoyao.node.listener.platform;

import com.acgist.taoyao.signal.event.platform.ShutdownEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

/**
 * 关闭服务监听
 * 
 * @author acgist
 */
public class ShutdownListener extends ApplicationListenerAdapter<ShutdownEvent> {

	@Override
	public void onApplicationEvent(ShutdownEvent event) {
		// TODO：关闭
	}

}
