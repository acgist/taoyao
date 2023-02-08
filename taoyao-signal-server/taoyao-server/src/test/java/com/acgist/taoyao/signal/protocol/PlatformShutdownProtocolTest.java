package com.acgist.taoyao.signal.protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.annotation.TaoyaoTest;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.main.TaoyaoApplication;
import com.acgist.taoyao.signal.protocol.platform.PlatformShutdownProtocol;

@TaoyaoTest(classes = TaoyaoApplication.class)
class PlatformShutdownProtocolTest {
	
	@Autowired
	private PlatformShutdownProtocol platformShutdownProtocol;

	@Test
	void testShutdown() {
		assertDoesNotThrow(() -> {
			this.platformShutdownProtocol.execute("taoyao", Message.success(), null);
			Thread.sleep(1000);
		});
	}
	
}
