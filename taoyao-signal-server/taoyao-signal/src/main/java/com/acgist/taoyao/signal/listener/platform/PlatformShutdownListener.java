package com.acgist.taoyao.signal.listener.platform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import com.acgist.taoyao.boot.annotation.EventListener;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.signal.event.platform.PlatformScriptEvent;
import com.acgist.taoyao.signal.event.platform.PlatformShutdownEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

/**
 * 关闭平台监听
 * 
 * @author acgist
 */
@EventListener
public class PlatformShutdownListener extends ApplicationListenerAdapter<PlatformShutdownEvent> {

	@Autowired
	private ApplicationContext context;
	@Autowired
	private ScriptProperties scriptProperties;
	
	@Override
	public void onApplicationEvent(PlatformShutdownEvent event) {
		if(this.context instanceof ConfigurableApplicationContext context) {
			// API关闭
			if(context.isActive()) {
				// 如果需要完整广播可以设置延时
				context.close();
			} else {
			}
		} else {
			// 命令关闭
			this.context.publishEvent(new PlatformScriptEvent(
				this.scriptProperties.getPlatformShutdown(),
				event.getMessage(),
				event.getSession()
			));
		}
	}

}
