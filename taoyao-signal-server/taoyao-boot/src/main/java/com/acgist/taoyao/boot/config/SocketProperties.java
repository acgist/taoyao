package com.acgist.taoyao.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Socket信令配置
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "Socket信令配置", description = "Socket信令配置")
@ConfigurationProperties(prefix = "taoyao.socket")
public class SocketProperties {

    /**
     * 机密策略
     * 
     * @author acgist
     */
    @Getter
    public enum Encrypt {
        
        // AES
        AES("AES/ECB/PKCS5Padding"),
        // DES
        DES("DES/ECB/PKCS5Padding"),
        // 明文
        PLAINTEXT(null);
        
        /**
         * 算法
         */
        private final String algo;

        private Encrypt(String algo) {
            this.algo = algo;
        }
        
    }

    @Schema(title = "是否启用", description = "是否启用")
	private Boolean enabled;
    @Schema(title = "监听地址", description = "监听地址")
	private String host;
    @Schema(title = "监听端口", description = "监听端口")
	private Integer port;
    @Schema(title = "加密策略", description = "加密策略")
    private Encrypt encrypt;
    @Schema(title = "加密密钥", description = "加密密钥：为空自动生成")
    private String encryptKey;
    @Schema(title = "超时时间", description = "超时时间")
	private Long timeout;
    @Schema(title = "队列长度", description = "队列长度")
	private Integer queueSize;
    @Schema(title = "最小线程数量", description = "最小线程数量")
	private Integer minThread;
    @Schema(title = "最大线程数量", description = "最大线程数量")
	private Integer maxThread;
    @Schema(title = "线程池的前缀", description = "线程池的前缀")
	private String threadNamePrefix;
    @Schema(title = "线程销毁时间", description = "线程销毁时间")
	private Long keepAliveTime;
    @Schema(title = "缓冲大小", description = "缓冲大小")
	private Integer bufferSize;
    @Schema(title = "最大缓冲大小", description = "最大缓冲大小")
    private Integer maxBufferSize;
	
}
