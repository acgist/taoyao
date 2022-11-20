package com.acgist.taoyao.signal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletionStage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketClient {

	public static final WebSocket build(String uri, String sn) throws InterruptedException {
		final Object lock = new Object();
		try {
			return HttpClient
				.newBuilder()
				.sslContext(newSSLContext())
				.build()
				.newWebSocketBuilder()
				.buildAsync(URI.create(uri), new Listener() {
					@Override
					public void onOpen(WebSocket webSocket) {
						webSocket.sendText(String.format("""
							{"header":{"pid":2000,"v":"1.0.0","id":"1","sn":"%s"},"body":{"username":"taoyao","password":"taoyao"}}
						""", sn), true);
						Listener.super.onOpen(webSocket);
					}
					@Override
					public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
						synchronized (lock) {
							lock.notifyAll();
						}
						log.info("收到WebSocket消息：{}", data);
						return Listener.super.onText(webSocket, data, last);
					}
				})
				.join();
		} finally {
			synchronized (lock) {
				lock.wait(1000);
			}
		}
	}
	
	private static final SSLContext newSSLContext() {
		SSLContext sslContext = null;
		try {
			// SSL协议：SSL、SSLv2、SSLv3、TLS、TLSv1、TLSv1.1、TLSv1.2、TLSv1.3
			sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(null, TRUST_ALL_CERT_MANAGER, new SecureRandom());
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			log.error("新建SSLContext异常", e);
			try {
				sslContext = SSLContext.getDefault();
			} catch (NoSuchAlgorithmException ex) {
				log.error("新建默认SSLContext异常", ex);
			}
		}
		return sslContext;
	}
	
	private static final TrustManager[] TRUST_ALL_CERT_MANAGER = new TrustManager[] {
		new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		}
	};
	
}
