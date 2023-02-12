package com.acgist.taoyao.signal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

public class SocketSignalTest {

	@Test
	void test() throws UnknownHostException, IOException {
		final Socket socket = new Socket();
		socket.connect(new InetSocketAddress("127.0.0.1", 9999));
		final OutputStream outputStream = socket.getOutputStream();
		outputStream.write("{}".getBytes());
		socket.close();
	}
	
}
