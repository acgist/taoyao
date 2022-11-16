package com.acgist.taoyao.signal.listener.platform;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.signal.event.platform.ScriptEvent;
import com.acgist.taoyao.signal.listener.ApplicationListenerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 执行命令监听
 * 
 * @author acgist
 */
@Slf4j
@Component
public class ScriptListener extends ApplicationListenerAdapter<ScriptEvent> {
	
	@Async
	@Override
	public void onApplicationEvent(ScriptEvent event) {
		final String script = (String) event.getBody().get("script");
		log.debug("执行命令：{}", script);
		this.execute(script);
	}

	/**
	 * 执行命令
	 * 
	 * @param script 命令
	 */
	private void execute(String script) {
		if(StringUtils.isEmpty(script)) {
			log.warn("执行命令失败：{}", script);
			return;
		}
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(script);
			try(
				final InputStream input = process.getInputStream();
				final InputStream error = process.getErrorStream();
			) {
				log.info("""
					执行命令：{}
					执行结果：{}
					失败结果：{}
					""", script, new String(input.readAllBytes()), new String(error.readAllBytes()));
			} catch (Exception e) {
				log.error("命令执行异常：{}", script, e);
			}
		} catch (Exception e) {
			log.error("执行命令异常：{}", script, e);
		} finally {
			if(process != null) {
				process.destroy();
//				process.destroyForcibly();
			}
		}
	}
	
}
