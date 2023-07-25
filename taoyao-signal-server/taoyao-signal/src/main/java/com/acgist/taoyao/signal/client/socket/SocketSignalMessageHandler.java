package com.acgist.taoyao.signal.client.socket;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import com.acgist.taoyao.boot.config.SocketProperties;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;
import com.acgist.taoyao.signal.utils.CipherUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Socket信令消息处理器
 * 消息格式：short message length + message
 * 
 * @author acgist
 */
@Slf4j
public final class SocketSignalMessageHandler implements CompletionHandler<Integer, ByteBuffer> {

    private final ClientManager         clientManager;
    private final ProtocolManager       protocolManager;
    private final PlatformErrorProtocol platformErrorProtocol;
    
    /**
     * 消息长度
     */
    private short messageLength;
    /**
     * 缓冲大小
     */
    private final int bufferSize;
    /**
     * 最大缓存大小
     */
    private final int maxBufferSize;
    /**
     * 加密工具
     */
    private final Cipher cipher;
    /**
     * 消息处理
     */
    private final ByteBuffer buffer;
    /**
     * 终端通道
     */
    private final AsynchronousSocketChannel channel;
    
    public SocketSignalMessageHandler(
        ClientManager             clientManager,
        ProtocolManager           protocolManager,
        SocketProperties          socketProperties,
        PlatformErrorProtocol     platformErrorProtocol,
        AsynchronousSocketChannel channel
    ) {
        this.clientManager         = clientManager;
        this.protocolManager       = protocolManager;
        this.platformErrorProtocol = platformErrorProtocol;
        this.channel               = channel;
        this.messageLength = 0;
        this.bufferSize    = socketProperties.getBufferSize();
        this.maxBufferSize = socketProperties.getMaxBufferSize();
        this.cipher        = CipherUtils.buildCipher(Cipher.DECRYPT_MODE, socketProperties.getEncrypt(), socketProperties.getEncryptSecret());
        this.buffer        = ByteBuffer.allocateDirect(maxBufferSize);
    }

    /**
     * 消息轮询
     */
    public void loopMessage() {
        if(this.channel.isOpen()) {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(this.bufferSize);
            this.channel.read(buffer, buffer, this);
        } else {
            log.debug("Socket信令消息轮询退出（通道已经关闭）");
            this.close();
        }
    }

    /**
     * 关闭通道
     */
    private void close() {
        log.debug("Socket信令终端关闭：{}", this.channel);
        this.clientManager.close(this.channel);
    }
    
    @Override
    public void completed(Integer result, ByteBuffer buffer) {
        if (result == null || result < 0) {
            log.warn("Socket信令接收消息失败关闭通道：{}", result);
            this.close();
        } else if(result == 0) {
            // 消息空轮询
            log.debug("Socket信令接收消息失败（长度）：{}", result);
        } else {
            buffer.flip();
            this.buffer.put(buffer);
            while(this.buffer.position() > 0) {
                if(this.messageLength <= 0) {
                    if(this.buffer.position() < Short.BYTES) {
                        // 不够消息长度
                        break;
                    } else {
                        this.buffer.flip();
                        this.messageLength = this.buffer.getShort();
                        this.buffer.compact();
                        if(this.messageLength < 0 || this.messageLength > this.maxBufferSize) {
                            throw MessageCodeException.of("信令消息长度错误：" + this.messageLength);
                        }
                    }
                } else {
                    if(this.buffer.position() < this.messageLength) {
                        // 不够消息长度
                        break;
                    } else {
                        // 拆包
                        final byte[] bytes = new byte[this.messageLength];
                        this.messageLength = 0;
                        this.buffer.flip();
                        this.buffer.get(bytes);
                        this.buffer.compact();
                        // 解密
                        final String message = this.decrypt(bytes);
                        log.debug("Socket信令消息：{} - {}", this.channel, message);
                        // 处理
                        this.execute(message);
                    }
                }
            }
        }
        this.loopMessage();
    }
    
    @Override
    public void failed(Throwable throwable, ByteBuffer buffer) {
        log.error("Socket信令终端异常：{}", this.channel, throwable);
        this.close();
    }
    
    /**
     * @param bytes 加密消息
     * 
     * @return 消息
     */
    private String decrypt(byte[] bytes) {
        if(this.cipher != null) {
            try {
                return new String(this.cipher.doFinal(bytes));
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                log.error("解密异常", e);
            }
        }
        return new String(bytes);
    }
    
    /**
     * @param message 消息
     */
    private void execute(String message) {
        try {
            this.protocolManager.execute(message, this.channel);
        } catch (Exception e) {
            log.error("处理Socket信令消息异常：{} - {}", this.clientManager.getClients(this.channel), message, e);
            this.clientManager.push(this.channel, this.platformErrorProtocol.build(e));
        }
    }
    
}