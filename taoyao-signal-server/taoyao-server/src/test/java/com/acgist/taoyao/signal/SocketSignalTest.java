package com.acgist.taoyao.signal;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.SocketProperties.Encrypt;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.utils.CipherUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocketSignalTest {
	
	@Test
	void testSocket() throws Exception {
		final Socket socket = new Socket();
		socket.connect(new InetSocketAddress("127.0.0.1", 9999));
		final AtomicInteger recvIndex = new AtomicInteger();
		final InputStream inputStream = socket.getInputStream();
		final OutputStream outputStream = socket.getOutputStream();
		// 随机密码：https://localhost:8888/config/socket
		final String secret = """
		    Oi7ZvxZEcOU=
		    """.strip();
		final Cipher encrypt = CipherUtils.buildCipher(Cipher.ENCRYPT_MODE, Encrypt.DES, secret);
		final Cipher decrypt = CipherUtils.buildCipher(Cipher.DECRYPT_MODE, Encrypt.DES, secret);
		// 接收
		new Thread(() -> {
			int length = 0;
			short messageLength = 0;
			final byte[] bytes = new byte[1024];
			final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
			try {
				while((length = inputStream.read(bytes)) >= 0) {
				    buffer.put(bytes, 0, length);
				    while(buffer.position() > 0) {
		                if(messageLength <= 0) {
		                    if(buffer.position() < Short.BYTES) {
		                        // 不够消息长度
		                        break;
		                    } else {
		                        buffer.flip();
		                        messageLength = buffer.getShort();
		                        buffer.compact();
		                        if(messageLength > 16 * 1024) {
		                            throw MessageCodeException.of("超过最大数据大小：" + messageLength);
		                        }
		                    }
		                } else {
		                    if(buffer.position() < messageLength) {
		                        // 不够消息长度
		                        break;
		                    } else {
		                        final byte[] message = new byte[messageLength];
		                        messageLength = 0;
		                        buffer.flip();
		                        buffer.get(message);
		                        buffer.compact();
		                        log.debug("收到消息：{}", new String(decrypt.doFinal(message)));
		                        recvIndex.incrementAndGet();
		                    }
		                }
		            }
				}
			} catch (Exception e) {
				log.error("读取异常", e);
			}
		}).start();
		// 发送
		final AtomicInteger sendIndex = new AtomicInteger();
		final Executor executor = Executors.newFixedThreadPool(10);
		for (int index = 0; index < 100; index++) {
			executor.execute(() -> {
				try {
				    final byte[] bytes = ("{\"time\":" + System.nanoTime() + "}").getBytes();
				    final byte[] encryptBytes = encrypt.doFinal(bytes);
				    final ByteBuffer buffer = ByteBuffer.allocateDirect(Short.BYTES + encryptBytes.length);
				    buffer.putShort((short) encryptBytes.length);
				    buffer.put(encryptBytes);
				    buffer.flip();
				    final byte[] message = new byte[buffer.capacity()];
				    buffer.get(message);
					outputStream.write(message);
					sendIndex.incrementAndGet();
				} catch (Exception e) {
					log.error("发送异常", e);
				}
			});
		}
		Thread.sleep(5000);
		log.info("发送数据：{}", sendIndex.get());
		log.info("接收数据：{}", recvIndex.get());
		socket.close();
	}
	
}
