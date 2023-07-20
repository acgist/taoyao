package com.acgist.taoyao.signal.client.socket;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.acgist.taoyao.boot.config.SocketProperties;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令终端接收器
 * 
 * @author acgist
 */
@Slf4j
public final class SocketSignalAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

    private final ClientManager         clientManager;
    private final ProtocolManager       protocolManager;
    private final SocketProperties      socketProperties;
    private final PlatformErrorProtocol platformErrorProtocol;
    
    public SocketSignalAcceptHandler(
        ClientManager         clientManager,
        ProtocolManager       protocolManager,
        SocketProperties      socketProperties,
        PlatformErrorProtocol platformErrorProtocol
    ) {
        this.clientManager         = clientManager;
        this.protocolManager       = protocolManager;
        this.socketProperties      = socketProperties;
        this.platformErrorProtocol = platformErrorProtocol;
    }

    @Override
    public void completed(AsynchronousSocketChannel channel, AsynchronousServerSocketChannel server) {
        try {
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
            this.clientManager.open(new SocketClient(this.socketProperties, channel));
            final SocketSignalMessageHandler messageHandler = new SocketSignalMessageHandler(
                this.clientManager,
                this.protocolManager,
                this.socketProperties,
                this.platformErrorProtocol,
                channel
            );
            messageHandler.loopMessage();
            log.debug("Socket信令终端连接成功：{}", channel);
        } catch (IOException e) {
            log.error("Socket信令终端连接异常：", channel, e);
        } finally {
            server.accept(server, this);
        }
    }
    
    @Override
    public void failed(Throwable throwable, AsynchronousServerSocketChannel server) {
        log.error("Socket信令终端连接异常：{}", server, throwable);
    }
    
}