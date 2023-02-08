package com.acgist.taoyao.node.listener.platform;

import com.acgist.taoyao.signal.event.platform.PlatformShutdownEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

/**
 * 关闭平台监听
 * 
 * TODO：节点关闭
 * 
 * @author acgist
 */
public class NodeShutdownListener extends ApplicationListenerAdapter<PlatformShutdownEvent> {

	@Override
	public void onApplicationEvent(PlatformShutdownEvent event) {
		// TODO：
	}

}
