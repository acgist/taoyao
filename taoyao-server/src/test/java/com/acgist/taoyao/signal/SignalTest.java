package com.acgist.taoyao.signal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.http.WebSocket;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.main.TaoyaoApplication;
import com.acgist.taoyao.test.annotation.TaoyaoTest;

@TaoyaoTest(classes = TaoyaoApplication.class)
class SignalTest {

	@Test
	void testSignal() throws InterruptedException {
		final WebSocket clientA = WebSocketClient.build("wss://localhost:8888/websocket.signal", "clientA");
		final WebSocket clientB = WebSocketClient.build("wss://localhost:8888/websocket.signal", "clientB");
		clientA.sendText("""
			{"header":{"pid":1000,"v":"1.0.0","id":"1","sn":"clientA"},"body":{}}
			""", true).join();
		assertNotNull(clientA);
		assertNotNull(clientB);
	}

}
