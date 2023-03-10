package com.acgist.taoyao.signal.client.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import com.acgist.taoyao.boot.config.SocketProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.ClientAdapter;
import com.acgist.taoyao.signal.utils.CipherUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Socket终端
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class SocketClient extends ClientAdapter<AsynchronousSocketChannel> {

    /**
     * 加密工具
     */
    private final Cipher cipher;
    
	public SocketClient(SocketProperties socketProperties, AsynchronousSocketChannel instance) {
		super(socketProperties.getTimeout(), instance);
		this.ip = this.clientIp(instance);
		this.cipher = CipherUtils.buildCipher(Cipher.ENCRYPT_MODE, socketProperties.getEncrypt(), socketProperties.getEncryptKey());
	}

	@Override
	public void push(Message message) {
	    synchronized (this.instance) {
	        try {
				if(this.instance.isOpen()) {
				    // 加密
					final byte[] bytes = this.encrypt(message);
					// 发送
					final ByteBuffer buffer = ByteBuffer.allocateDirect(Short.BYTES + bytes.length);
					buffer.putShort((short) bytes.length);
					buffer.put(bytes);
					buffer.flip();
					final Future<Integer> future = this.instance.write(buffer);
					future.get(this.timeout, TimeUnit.MILLISECONDS);
				} else {
					log.error("Socket终端已经关闭：{}", this.instance);
				}
			} catch (Exception e) {
			    log.error("Socket终端发送消息异常：{}", message, e);
			}
		}
	}
	
	/**
	 * @param instance 终端实例
	 * 
	 * @return 终端IP
	 */
	private String clientIp(AsynchronousSocketChannel instance) {
	    try {
            return ((InetSocketAddress) instance.getRemoteAddress()).getHostString();
        } catch (IOException e) {
            throw MessageCodeException.of(e, "无效终端（IP）：" + instance);
        }
	}
	
	/**
	 * @param message 消息
	 * 
	 * @return 加密消息
	 */
	private byte[] encrypt(Message message) {
	    final byte[] bytes = message.toString().getBytes();
        if(this.cipher != null) {
            try {
                return this.cipher.doFinal(bytes);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                log.error("加密异常：{}", message);
            }
        }
        return bytes;
	}
	
}
