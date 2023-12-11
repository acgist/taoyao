package com.acgist.taoyao.boot.utils;

import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Function;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP工具
 * 
 * @author acgist
 */
@Slf4j
public final class HTTPUtils {

    /**
     * 超时时间
     */
    private static long timeout;
    /**
     * 线程池
     */
    private static Executor executor;
    /**
     * 无效IP验证
     */
    private static final Function<String, Boolean> IP_CHECKER = ip -> StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip);
    
    private HTTPUtils() {
    }
    
    /**
     * @param timeout  超时时间
     * @param executor 线程池
     */
    public static final void init(long timeout, Executor executor) {
        HTTPUtils.timeout  = timeout;
        HTTPUtils.executor = executor;
    }
    
    /**
     * @return HTTPClient
     */
    public static final HttpClient newClient() {
        return HttpClient
            .newBuilder()
//          .version(Version.HTTP_2)
            .executor(HTTPUtils.executor)
            .sslContext(HTTPUtils.buildSSLContext())
            .connectTimeout(Duration.ofMillis(HTTPUtils.timeout))
//          .followRedirects(Redirect.ALWAYS)
            .build();
    }
    
    /**
     * @param request HTTP请求
     * 
     * @return IP地址
     */
    public static final String clientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (IP_CHECKER.apply(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (IP_CHECKER.apply(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
 
    /**
     * @return {@link SSLContext}
     */
    private static final SSLContext buildSSLContext() {
        try {
            // SSL协议：SSL、SSLv2、SSLv3、TLS、TLSv1、TLSv1.1、TLSv1.2、TLSv1.3
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(
                new KeyManager[0],
                new X509TrustManager[] {
                    TaoyaoTrustManager.INSTANCE
                },
                new SecureRandom()
            );
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
