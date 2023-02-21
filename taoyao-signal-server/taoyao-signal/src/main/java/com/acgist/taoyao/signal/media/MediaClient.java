package com.acgist.taoyao.signal.media;

import java.net.URI;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;

import com.acgist.taoyao.boot.annotation.Prototype;
import com.acgist.taoyao.boot.config.MediaServerProperties;
import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.HTTPUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.signal.protocol.Protocol;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.media.MediaRegisterProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务终端
 * 
 * @author acgist
 */
@Slf4j
@Prototype
public class MediaClient {
	
	@Autowired
	private TaskScheduler taskScheduler;
	@Autowired
	private ProtocolManager protocolManager;
	@Autowired
	private TaoyaoProperties taoyaoProperties;
	@Autowired
	private MediaRegisterProtocol mediaRegisterProtocol;

	/**
	 * 最长重试周期
	 */
	private static final long MAX_DURATION = 60L * 1000;
	
	/**
	 * 标识
	 */
	private String mediaId;
	/**
	 * 重试周期
	 */
	private long duration;
	/**
	 * 服务通道
	 */
	private WebSocket webSocket;
	/**
	 * 服务配置
	 */
	private MediaServerProperties mediaServerProperties;
	/**
	 * 同步消息
	 */
	private final Map<String, Message> requestMessage = new ConcurrentHashMap<>();

	/**
	 * 加载终端
	 * 
	 * @param mediaServerProperties 媒体服务配置
	 */
	public void init(MediaServerProperties mediaServerProperties) {
		this.mediaId = mediaServerProperties.getMediaId();
		this.duration = this.taoyaoProperties.getTimeout();
		this.mediaServerProperties = mediaServerProperties;
		this.connectServer();
	}
	
	/**
	 * @return 标识
	 */
	public String mediaId() {
		return this.mediaId;
	}
	
	/**
	 * @return 媒体服务配置
	 */
	public MediaServerProperties mediaServerProperties() {
	    return this.mediaServerProperties;
	}

	/**
	 * 心跳
	 */
	public void heartbeat() {
	    final CompletableFuture<WebSocket> future = this.webSocket.sendPing(ByteBuffer.allocate(0));
	    try {
	        log.debug("媒体服务心跳：{}", this.mediaId);
            future.get(this.taoyaoProperties.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("媒体服务心跳异常：{}", this.mediaId, e);
        }
	}
	
	/**
	 * 发送消息
	 * 
	 * @param message 消息
	 */
	public void send(Message message) {
		while(this.webSocket == null) {
			Thread.yield();
		}
		this.webSocket.sendText(JSONUtils.toJSON(message), true);
	}
	
	/**
	 * 请求消息
	 * 
	 * @param request 消息
	 * 
	 * @return 响应
	 */
	public Message request(Message request) {
	    final Header header = request.getHeader();
		final String id = header.getId();
		this.requestMessage.put(id, request);
		synchronized (request) {
			this.send(request);
			try {
				request.wait(this.taoyaoProperties.getTimeout());
			} catch (InterruptedException e) {
				log.error("媒体服务等待响应异常：{}", request, e);
			}
		}
		final Message response = this.requestMessage.remove(id);
		if(response == null || request.equals(response)) {
			log.warn("媒体服务没有响应：{}", request);
			throw MessageCodeException.of(MessageCode.CODE_2001, "媒体服务没有响应");
		}
		return response;
	}
	
	/**
	 * @return 重试周期
	 */
	private long retryDuration() {
		return this.duration = Math.min(this.duration + this.taoyaoProperties.getTimeout(), MAX_DURATION);
	}
	
    /**
     * 连接服务通道
     */
    private void connectServer() {
        final URI uri = URI.create(this.mediaServerProperties.getAddress());
        log.debug("开始连接媒体服务：{}", uri);
        try {
            final WebSocket webSocket = HTTPUtils.newClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofMillis(this.taoyaoProperties.getTimeout()))
                .buildAsync(uri, new MessageListener())
                .get();
            log.info("连接媒体服务成功：{}", webSocket);
            // 关闭旧的通道
            if(this.webSocket != null && !(this.webSocket.isInputClosed() && this.webSocket.isOutputClosed())) {
                this.webSocket.abort();
            }
            // 设置新的通道
            this.webSocket = webSocket;
            // 重置重试周期
            this.duration = this.taoyaoProperties.getTimeout();
            // 发送授权消息
            this.send(this.mediaRegisterProtocol.build(this.mediaServerProperties));
        } catch (Exception e) {
            log.error("连接媒体服务异常：{}", uri, e);
            this.taskScheduler.schedule(
                this::connectServer,
                Instant.now().plusMillis(this.retryDuration())
            );
        }
    }
	
	/**
	 * 处理信令消息
	 * 
	 * @param content 信令消息
	 */
	private void execute(String content) {
		if(StringUtils.isEmpty(content)) {
		    log.warn("媒体服务信令消息格式错误：{}", content);
		    return;
		}
		final Message message = JSONUtils.toJava(content, Message.class);
		final Header header = message.getHeader();
		if(header == null) {
		    log.warn("媒体服务信令消息格式错误（没有头部）：{}", content);
		    return;
		}
		final String v = header.getV();
		final String id = header.getId();
		final String signal = header.getSignal();
		if(v == null || id == null || signal == null) {
		    log.warn("媒体服务信令消息格式错误（缺失头部关键参数）：{}", content);
		    return;
		}
		final Message request = this.requestMessage.get(id);
		if(request != null) {
		    // 同步处理：重新设置响应消息
		    this.requestMessage.put(id, message);
		    // 唤醒等待线程
		    synchronized (request) {
		        request.notifyAll();
		    }
		    // 同步处理不要执行回调
		} else {
		    final Protocol protocol = this.protocolManager.protocol(signal);
		    if(protocol == null) {
		        log.warn("不支持的媒体信令协议：{}", content);
		    } else {
		        protocol.execute(this, message);
		    }
		}
	}
	
	/**
	 * 信令消息监听
	 * 
	 * @author acgist
	 */
	public class MessageListener implements Listener {
		
		@Override
    	public void onOpen(WebSocket webSocket) {
    		log.info("媒体服务通道打开：{}", webSocket);
    		Listener.super.onOpen(webSocket);
    	}
		
    	@Override
    	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer buffer, boolean last) {
    		log.debug("媒体服务收到信令消息（binary）：{}", webSocket);
    		return Listener.super.onBinary(webSocket, buffer, last);
    	}
    	
    	@Override
    	public CompletionStage<?> onText(WebSocket webSocket, CharSequence content, boolean last) {
    		log.debug("媒体服务收到信令消息（text）：{}-{}", webSocket, content);
    		try {
    			MediaClient.this.execute(content.toString());
			} catch (Exception e) {
				log.error("处理媒体服务信令消息异常：{}", content, e);
			}
    		return Listener.super.onText(webSocket, content, last);
    	}
    	
    	@Override
    	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    		log.warn("媒体服务通道关闭：{}-{}-{}", webSocket, statusCode, reason);
    		try {
    			return Listener.super.onClose(webSocket, statusCode, reason);
			} finally {
				MediaClient.this.taskScheduler.schedule(
					MediaClient.this::connectServer,
					Instant.now().plusMillis(MediaClient.this.retryDuration())
				);
			}
    	}
    	
    	@Override
    	public void onError(WebSocket webSocket, Throwable error) {
    		log.error("媒体服务通道异常：{}", webSocket, error);
    		try {
    			Listener.super.onError(webSocket, error);
			} finally {
				MediaClient.this.taskScheduler.schedule(
					MediaClient.this::connectServer,
					Instant.now().plusMillis(MediaClient.this.retryDuration())
				);
			}
    	}
    	
    	@Override
    	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer buffer) {
    		log.debug("媒体服务收到信令消息（ping）：{}", webSocket);
    		return Listener.super.onPing(webSocket, buffer);
    	}
    	
    	@Override
    	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer buffer) {
    		log.debug("媒体服务收到信令消息（pong）：{}", webSocket);
    		return Listener.super.onPong(webSocket, buffer);
    	}
    	
	}
	
}
