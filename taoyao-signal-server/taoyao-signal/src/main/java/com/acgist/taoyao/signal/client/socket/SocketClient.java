package com.acgist.taoyao.signal.client.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.client.ClientAdapter;

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
	 * 换行符号
	 */
	private final byte[] line;
	/**
	 * 换行符号长度
	 */
	private final int lineLength;
	
	public SocketClient(Long timeout, AsynchronousSocketChannel instance) {
		super(timeout, instance);
		this.ip = this.clientIp(instance);
		this.line = Constant.LINE.getBytes();
		this.lineLength = this.line.length;
	}

	@Override
	public void push(Message message) {
	    synchronized (this.instance) {
	        try {
				if(this.instance.isOpen()) {
					final byte[] bytes = message.toString().getBytes();
					final ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length + this.lineLength);
					buffer.put(bytes);
					buffer.put(this.line);
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
	
}
