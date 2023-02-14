package com.acgist.taoyao.signal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.signal.protocol.Constant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocketSignalTest {
	
	@Test
	void testSocket() throws Exception {
		final Socket socket = new Socket();
		socket.connect(new InetSocketAddress("127.0.0.1", 9999));
		final OutputStream outputStream = socket.getOutputStream();
		final InputStream inputStream = socket.getInputStream();
		final String line = Constant.LINE;
		final int lineLength = line.length();
		new Thread(() -> {
			int index = 0;
			int length = 0;
			final byte[] bytes = new byte[1024];
			final StringBuilder builder = new StringBuilder();
			try {
				while((length = inputStream.read(bytes)) >= 0) {
					builder.append(new String(bytes, 0, length));
					while((index = builder.indexOf(line)) >= 0) {
						log.info("收到消息：{}", builder.substring(0, index));
						builder.delete(0, index + lineLength);
					}
				}
			} catch (IOException e) {
				log.error("读取异常", e);
			}
		}).start();
		final Executor executor = Executors.newFixedThreadPool(10);
		for (int index = 0; index < 100; index++) {
			executor.execute(() -> {
				try {
					outputStream.write(("{}" + line).getBytes());
				} catch (IOException e) {
					log.error("发送异常", e);
				}
			});
		}
		Thread.sleep(5000);
		socket.close();
	}
	
}
