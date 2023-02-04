package com.acgist.taoyao.mediasoup;

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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.property.MediasoupProperties;
import com.acgist.taoyao.boot.property.TaoyaoProperties;
import com.acgist.taoyao.boot.property.WebrtcProperties;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.mediasoup.protocol.ProtocolMediasoupAdapter;
import com.acgist.taoyao.mediasoup.protocol.client.AuthorizeProtocol;
import com.acgist.taoyao.signal.protocol.Protocol;
import com.acgist.taoyao.signal.protocol.ProtocolManager;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Mediasoup客户端
 * 
 * @author acgist
 */
@Slf4j
@Service
public class MediasoupClient {
	
	@Autowired
	private TaskScheduler taskSchedulerl;
	@Autowired
	private ProtocolManager protocolManager;
	@Autowired
	private TaoyaoProperties taoyaoProperties;
	@Autowired
	private WebrtcProperties webrtcProperties;
	@Autowired
	private AuthorizeProtocol authorizeProtocol;
	
	/**
	 * Mediasoup WebSocket通道
	 */
	private WebSocket webSocket;
	/**
	 * Mediasoup配置
	 */
	private MediasoupProperties mediasoupProperties;
	/**
	 * 同步消息
	 */
	private Map<String, Message> syncMessage = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		this.mediasoupProperties = this.webrtcProperties.getMediasoup();
		this.buildClient();
	}
	
	/**
	 * 连接Mediasoup WebSocket通道
	 */
	public void buildClient() {
		final URI uri = URI.create(this.mediasoupProperties.getAddress());
		log.info("开始连接Mediasoup：{}", uri);
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
			log.error("连接Mediasoup异常：{}", uri, e);
			this.taskSchedulerl.schedule(this::buildClient, Instant.now().plusSeconds(5));
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
	 * 同步发送消息
	 * 
	 * @param message 消息
	 * 
	 * @return 响应
	 */
	public Message sendSync(Message message) {
		final String id = message.getHeader().getId();
		this.syncMessage.put(id, message);
		synchronized (message) {
			try {
				message.wait(this.taoyaoProperties.getTimeout());
			} catch (InterruptedException e) {
				log.error("等待同步消息异常：{}", message, e);
			}
		}
		final Message response = this.syncMessage.remove(id);
		if(response == null || message.equals(response)) {
			log.warn("消息没有响应：{}", message);
		}
		return response;
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
			final String id = header.getId();
			final Integer pid = header.getPid();
			final Message request = this.syncMessage.get(id);
			// 存在同步响应
			if(request != null) {
				// 重新设置消息
				this.syncMessage.put(id, message);
				// 唤醒等待现场
				synchronized (request) {
					request.notifyAll();
				}
			} else {
				final Protocol protocol = this.protocolManager.protocol(pid);
				if(protocol instanceof ProtocolMediasoupAdapter mediasoupProtocol) {
					mediasoupProtocol.execute(message, this.webSocket);
				} else {
					log.warn("未知Mediasoup信令：{}", data);
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
    		log.info("Mediasoup通道打开：{}", webSocket);
    		Listener.super.onOpen(webSocket);
    		// 关闭旧的通道
    		if(MediasoupClient.this.webSocket != null && !(MediasoupClient.this.webSocket.isInputClosed() && MediasoupClient.this.webSocket.isOutputClosed())) {
    			MediasoupClient.this.webSocket.abort();
    		}
    		// 设置新的通道
    		MediasoupClient.this.webSocket = webSocket;
    		// 发送授权消息
    		MediasoupClient.this.send(MediasoupClient.this.authorizeProtocol.build());
    	}
		
    	@Override
    	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
    		log.debug("Mediasoup收到消息（binary）：{}", webSocket);
    		return Listener.super.onBinary(webSocket, data, last);
    	}
    	
    	@Override
    	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    		log.debug("Mediasoup收到消息（text）：{}-{}", webSocket, data);
    		MediasoupClient.this.execute(data.toString());
    		return Listener.super.onText(webSocket, data, last);
    	}
    	
    	@Override
    	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    		log.warn("Mediasoup通道关闭：{}-{}-{}", webSocket, statusCode, reason);
    		try {
    			return Listener.super.onClose(webSocket, statusCode, reason);
			} finally {
				MediasoupClient.this.taskSchedulerl.schedule(MediasoupClient.this::buildClient, Instant.now().plusSeconds(5));
			}
    	}
    	
    	@Override
    	public void onError(WebSocket webSocket, Throwable error) {
    		log.error("Mediasoup通道异常：{}", webSocket, error);
    		try {
    			Listener.super.onError(webSocket, error);
			} finally {
				MediasoupClient.this.taskSchedulerl.schedule(MediasoupClient.this::buildClient, Instant.now().plusSeconds(5));
			}
    	}
    	
    	@Override
    	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
    		log.debug("Mediasoup收到消息（ping）：{}", webSocket);
    		return Listener.super.onPing(webSocket, message);
    	}
    	
    	@Override
    	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    		log.debug("Mediasoup收到消息（pong）：{}", webSocket);
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
