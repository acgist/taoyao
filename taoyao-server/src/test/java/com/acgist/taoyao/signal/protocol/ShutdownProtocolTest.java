package com.acgist.taoyao.signal.protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.main.TaoyaoApplication;
import com.acgist.taoyao.signal.protocol.platform.ShutdownProtocol;
import com.acgist.taoyao.test.annotation.TaoyaoTest;

@TaoyaoTest(classes = TaoyaoApplication.class)
class ShutdownProtocolTest {
	
	@Autowired
	private ShutdownProtocol shutdownProtocol;

	@Test
	void testShutdown() {
		assertDoesNotThrow(() -> {
			this.shutdownProtocol.execute("taoyao", null, null);
		});
	}
	
}
