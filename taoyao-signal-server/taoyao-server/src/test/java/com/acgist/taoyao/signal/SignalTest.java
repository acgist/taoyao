package com.acgist.taoyao.signal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.annotation.CostedTest;
import com.acgist.taoyao.annotation.TaoyaoTest;
import com.acgist.taoyao.main.TaoyaoApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TaoyaoTest(classes = TaoyaoApplication.class)
class SignalTest {

	/**
	 * 防止GC
	 */
	private List<WebSocket> list = new ArrayList<>();
	
	@Test
	void testSignal() throws InterruptedException {
		final WebSocket clientA = WebSocketClient.build("wss://localhost:8888/websocket.signal", "clientA");
		final WebSocket clientB = WebSocketClient.build("wss://localhost:8888/websocket.signal", "clientB");
		clientA.sendText("""
			{"header":{"signal":"client::heartbeat","v":"1.0.0","id":"1"},"body":{}}
		""", true).join();
		assertNotNull(clientA);
		assertNotNull(clientB);
	}

	@Test
	@CostedTest(thread = 10, count = 100, waitRelease = 5000L)
	void testThread() throws InterruptedException {
		final int total = 100;
		final CountDownLatch count = new CountDownLatch(total);
		final WebSocket clientA = WebSocketClient.build("wss://localhost:8888/websocket.signal", "clientA", count);
		final long aTime = System.currentTimeMillis();
		for (int index = 0; index < total; index++) {
			clientA.sendText("""
				{"header":{"signal":"client::status","v":"1.0.0","id":"1"},"body":{}}
			""", true).join();
		}
		this.list.add(clientA);
//		final ExecutorService executor = Executors.newFixedThreadPool(10);
//		for (int index = 0; index < total; index++) {
//			executor.execute(() -> {
//				synchronized (clientA) {
//					clientA.sendText("""
//						{"header":{"signal":"client::status","v":"1.0.0","id":"1"},"body":{}}
//					""", true).join();
//				}
//			});
//		}
		count.await();
		final long zTime = System.currentTimeMillis();
		log.info("执行时间：{}", zTime - aTime);
		log.info("当前连接数量：{}", this.list.size());
		assertNotNull(clientA);
	}
	
	@Test
	void testMax() throws InterruptedException {
		final int size = 1024;
		final CountDownLatch count = new CountDownLatch(size);
		for (int index = 0; index < size; index++) {
			final WebSocket clientA = WebSocketClient.build("wss://localhost:8888/websocket.signal", "clientA", count);
			assertNotNull(clientA);
			assertTrue(!(clientA.isInputClosed() || clientA.isOutputClosed()));
		}
		count.await();
	}

}
