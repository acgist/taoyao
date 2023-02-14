package com.acgist.taoyao.boot.utils;

import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import lombok.extern.slf4j.Slf4j;

/**
 * HTTP工具
 * 
 * @author acgist
 */
@Slf4j
public final class HTTPUtils {

    private HTTPUtils() {
    }
    
    /**
     * @return HTTPClient
     */
    public static final HttpClient newClient() {
        return HttpClient
            .newBuilder()
            .sslContext(buildSSLContext())
            .build();
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
