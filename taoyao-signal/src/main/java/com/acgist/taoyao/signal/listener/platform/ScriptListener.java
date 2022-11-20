package com.acgist.taoyao.signal.listener.platform;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
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
		final Message message = event.getMessage();
		final ClientSession session = event.getSession();
		final Map<?, ?> body = event.getBody();
		final String script = (String) body.get("script");
		log.debug("执行命令：{}", script);
		final String result = this.execute(script);
		message.setBody(Map.of("result", result));
		session.push(message);
	}

	/**
	 * 执行命令
	 * 
	 * @param script 命令
	 * 
	 * @return 执行结果
	 */
	private String execute(String script) {
		if(StringUtils.isEmpty(script)) {
			log.warn("执行命令失败：{}", script);
			return "命令为空";
		}
		String result = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(script);
			try(
				final InputStream input = process.getInputStream();
				final InputStream error = process.getErrorStream();
			) {
				final String inputValue = new String(input.readAllBytes());
				final String errorValue = new String(input.readAllBytes());
				log.info("""
					执行命令：{}
					执行结果：{}
					失败结果：{}
					""", script, inputValue, errorValue);
				result = StringUtils.isEmpty(inputValue) ? errorValue : inputValue;
			} catch (Exception e) {
				log.error("命令执行异常：{}", script, e);
				result = e.getMessage();
			}
		} catch (Exception e) {
			log.error("执行命令异常：{}", script, e);
			result = e.getMessage();
		} finally {
			if(process != null) {
				process.destroy();
			}
		}
		return result;
	}
	
}
