package com.acgist.taoyao.signal.protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.annotation.TaoyaoTest;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.main.TaoyaoApplication;
import com.acgist.taoyao.signal.protocol.platform.PlatformScriptProtocol;

@TaoyaoTest(classes = TaoyaoApplication.class)
class PlatformScriptProtocolTest {
	
	@Autowired
	private PlatformScriptProtocol platformScriptProtocol;

	@Test
	void testScript() {
		assertDoesNotThrow(() -> {
			this.platformScriptProtocol.execute("taoyao", Map.of("script", "netstat -ano"), null, Message.success());
			Thread.sleep(1000);
		});
	}
	
}
