package com.acgist.taoyao.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server {

    public static final Executor EXECUTOR = Executors.newCachedThreadPool();
    
    @Test
    public void testServer() throws Exception {
        final ServerSocket server = new ServerSocket(9999);
        while(!server.isClosed()) {
            final Socket accept = server.accept();
            EXECUTOR.execute(() -> {
                try {
                    this.execute(accept);
                } catch (IOException e) {
                    log.error("异常", e);
                }
            });
        }
        server.close();
    }
    
    public void execute(Socket accept) throws IOException {
        final InputStream inputStream = accept.getInputStream();
        final OutputStream outputStream = accept.getOutputStream();
        while(!accept.isClosed()) {
            final byte[] bytes = new byte[1024];
            final int length = inputStream.read(bytes);
            log.info("收到消息：{}", new String(bytes, 0, length));
            outputStream.write(bytes, 0, length);
            outputStream.flush();
        }
    }
    
}
