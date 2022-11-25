package com.acgist.taoyao.signal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.http.WebSocket;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.main.TaoyaoApplication;
import com.acgist.taoyao.test.annotation.TaoyaoTest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	@Test
	void testThread() throws InterruptedException {
		final int total = 1000;
		final CountDownLatch count = new CountDownLatch(total);
		final WebSocket clientA = WebSocketClient.build("wss://localhost:8888/websocket.signal", "clientA", count);
		final long aTime = System.currentTimeMillis();
		for (int index = 0; index < total; index++) {
			clientA.sendText("""
				{"header":{"pid":2999,"v":"1.0.0","id":"1","sn":"clientA"},"body":{}}
			""", true).join();
		}
//		final ExecutorService executor = Executors.newFixedThreadPool(10);
//		for (int index = 0; index < total; index++) {
//			executor.execute(() -> {
//				synchronized (clientA) {
//					clientA.sendText("""
//						{"header":{"pid":2999,"v":"1.0.0","id":"1","sn":"clientA"},"body":{}}
//					""", true).join();
//				}
//			});
//		}
		count.await();
		final long zTime = System.currentTimeMillis();
		log.info("执行时间：{}", zTime - aTime);
		Thread.sleep(1000);
		assertNotNull(clientA);
	}

}
