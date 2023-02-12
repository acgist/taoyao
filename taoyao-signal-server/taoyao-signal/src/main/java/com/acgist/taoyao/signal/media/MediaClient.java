package com.acgist.taoyao.signal.media;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;

import com.acgist.taoyao.boot.annotation.Client;
import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.property.MediaServerProperties;
import com.acgist.taoyao.boot.property.TaoyaoProperties;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.signal.protocol.Protocol;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.ProtocolMediaAdapter;
import com.acgist.taoyao.signal.protocol.media.MediaRegisterProtocol;

import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务终端
 * 
 * @author acgist
 */
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Client
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
	 * 名称
	 */
	private String name;
	/**
	 * 重试周期
	 */
	private long duration;
	/**
	 * 通道
	 */
	private WebSocket webSocket;
	/**
	 * 配置
	 */
	private MediaServerProperties mediaServerProperties;
	/**
	 * 同步消息
	 */
	private final Map<String, Message> syncMessage = new ConcurrentHashMap<>();

	/**
	 * 加载终端
	 * 
	 * @param mediaServerProperties 媒体服务配置
	 */
	public void init(MediaServerProperties mediaServerProperties) {
		this.mediaServerProperties = mediaServerProperties;
		this.name = mediaServerProperties.getName();
		this.duration = this.taoyaoProperties.getTimeout();
		this.buildClient();
	}
	
	/**
	 * @return 名称
	 */
	public String name() {
		return this.name;
	}
	
	/**
	 * 连接WebSocket通道
	 */
	public void buildClient() {
		final URI uri = URI.create(this.mediaServerProperties.getAddress());
		log.info("连接媒体服务：{}", uri);
		try {
			HttpClient
				.newBuilder()
				.sslContext(buildSSLContext())
				.build()
				.newWebSocketBuilder()
				.connectTimeout(Duration.ofMillis(this.taoyaoProperties.getTimeout()))
				.buildAsync(uri, new MessageListener())
				.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("连接媒体服务异常：{}", uri, e);
			this.taskScheduler.schedule(
				this::buildClient,
				Instant.now().plusMillis(this.retryDuration())
			);
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
	 * @param message 消息
	 * 
	 * @return 响应
	 */
	public Message sendSync(Message message) {
		final String id = message.getHeader().getId();
		this.syncMessage.put(id, message);
		synchronized (message) {
			this.send(message);
			try {
				message.wait(this.taoyaoProperties.getTimeout());
			} catch (InterruptedException e) {
				log.error("等待媒体服务响应异常：{}", message, e);
			}
		}
		final Message response = this.syncMessage.remove(id);
		if(response == null || message.equals(response)) {
			log.warn("媒体服务没有响应：{}", message);
			throw MessageCodeException.of(MessageCode.CODE_2001, "媒体服务没有响应");
		}
		return response;
	}
	
	/**
	 * 打开通道
	 * 
	 * @param webSocket 通道
	 */
	private void open(WebSocket webSocket) {
		// 关闭旧的通道
		if(this.webSocket != null && !(this.webSocket.isInputClosed() && this.webSocket.isOutputClosed())) {
			this.webSocket.abort();
		}
		// 重置重试周期
		this.duration = this.taoyaoProperties.getTimeout();
		// 设置新的通道
		this.webSocket = webSocket;
		// 发送授权消息
		this.send(this.mediaRegisterProtocol.build(this.mediaServerProperties));
	}
	
	/**
	 * @return 重试周期
	 */
	private long retryDuration() {
		return this.duration = Math.min(this.duration + this.taoyaoProperties.getTimeout(), this.taoyaoProperties.getMaxTimeout());
	}
	
	/**
	 * 处理消息
	 * 
	 * @param data 消息
	 */
	private void execute(String data) {
		if(StringUtils.isNotEmpty(data)) {
			final Message message = JSONUtils.toJava(data, Message.class);
			final Header header = message.getHeader();
			if(header == null) {
				log.warn("消息格式错误（没有头部）：{}", message);
				return;
			}
			final String v = header.getV();
			final String id = header.getId();
			final String signal = header.getSignal();
			if(v == null || id == null || signal == null) {
				log.warn("消息格式错误（缺失头部关键参数）：{}", message);
				return;
			}
			final Message request = this.syncMessage.get(id);
			if(request != null) {
				// 同步处理
				// 重新设置消息
				this.syncMessage.put(id, message);
				// 唤醒等待现场
				synchronized (request) {
					request.notifyAll();
				}
				// 同步处理不要执行回调
			} else {
				final Protocol protocol = this.protocolManager.protocol(signal);
				if(protocol instanceof ProtocolMediaAdapter protocolMediaAdapter) {
					protocolMediaAdapter.execute(message, this.webSocket);
				} else {
					log.warn("未知媒体服务信令：{}", data);
				}
			}
		}
	}
	
	/**
	 * 消息监听
	 * 
	 * @author acgist
	 */
	public class MessageListener implements Listener {
		
		@Override
    	public void onOpen(WebSocket webSocket) {
    		log.info("媒体服务通道打开：{}", webSocket);
    		Listener.super.onOpen(webSocket);
    		MediaClient.this.open(webSocket);
    	}
		
    	@Override
    	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
    		log.debug("媒体服务收到消息（binary）：{}", webSocket);
    		return Listener.super.onBinary(webSocket, data, last);
    	}
    	
    	@Override
    	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    		log.debug("媒体服务收到消息（text）：{}-{}", webSocket, data);
    		try {
    			MediaClient.this.execute(data.toString());
			} catch (Exception e) {
				log.error("媒体服务处理异常：{}", data, e);
			}
    		return Listener.super.onText(webSocket, data, last);
    	}
    	
    	@Override
    	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    		log.warn("媒体服务通道关闭：{}-{}-{}", webSocket, statusCode, reason);
    		try {
    			return Listener.super.onClose(webSocket, statusCode, reason);
			} finally {
				MediaClient.this.taskScheduler.schedule(
					MediaClient.this::buildClient,
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
					MediaClient.this::buildClient,
					Instant.now().plusMillis(MediaClient.this.retryDuration())
				);
			}
    	}
    	
    	@Override
    	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
    		log.debug("媒体服务收到消息（ping）：{}", webSocket);
    		return Listener.super.onPing(webSocket, message);
    	}
    	
    	@Override
    	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    		log.debug("媒体服务收到消息（pong）：{}", webSocket);
    		return Listener.super.onPong(webSocket, message);
    	}
    	
	}
	
	/**
	 * SSLContext
	 * 
	 * @return {@link SSLContext}
	 */
	private static final SSLContext buildSSLContext() {
		try {
			// SSL协议：SSL、SSLv2、SSLv3、TLS、TLSv1、TLSv1.1、TLSv1.2、TLSv1.3
			final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(null, new X509TrustManager[] { TaoyaoTrustManager.INSTANCE }, new SecureRandom());
			return sslContext;
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			log.error("新建SSLContext异常", e);
		}
		try {
			return SSLContext.getDefault();
		} catch (NoSuchAlgorithmException e) {
			log.error("新建SSLContext异常", e);
		}
		return null;
	}

	/**
	 * 证书验证
	 * 
	 * @author acgist
	 */
	public static class TaoyaoTrustManager implements X509TrustManager {

		private static final TaoyaoTrustManager INSTANCE = new TaoyaoTrustManager();

		private TaoyaoTrustManager() {
		}
		
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			if(chain == null) {
				throw new CertificateException("证书验证失败");
			}
		}
		
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			if(chain == null) {
				throw new CertificateException("证书验证失败");
			}
		}
		
	}
	
}
