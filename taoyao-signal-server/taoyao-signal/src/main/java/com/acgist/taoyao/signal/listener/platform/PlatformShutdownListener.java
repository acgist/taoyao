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
	private ScriptProperties scriptProperties;
	@Autowired
	private ApplicationContext applicationContext;
	
	@Override
	public void onApplicationEvent(PlatformShutdownEvent event) {
		if(this.applicationContext instanceof ConfigurableApplicationContext context) {
			// API关闭
			if(context.isActive()) {
				// 如果需要完整广播可以设置延时
				context.close();
			} else {
			}
		} else {
			// 命令关闭
			this.applicationContext.publishEvent(new PlatformScriptEvent(
				this.scriptProperties.getPlatformShutdown(),
				event.getMessage(),
				event.getClient()
			));
		}
	}

}
