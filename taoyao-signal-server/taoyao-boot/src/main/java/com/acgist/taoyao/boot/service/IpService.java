package com.acgist.taoyao.boot.service;

/**
 * 内外网多网卡环境IP服务
 * 支持配置域名
 * 
 * @author acgist
 */
public interface IpService {
    
    /**
     * @see #subnetIp(String, String, String)
     */
    default boolean subnetIp(String sourceIp, String clientIp) {
        return this.subnetIp(sourceIp, clientIp, null);
    }
    
    /**
     * 内网是否相同网段
     * 
     * @param sourceIp 原始IP
     * @param clientIp 终端IP
     * @param defaultSourceIp 默认原始IP
     * 
     * @return 是否匹配
     */
    boolean subnetIp(String sourceIp, String clientIp, String defaultSourceIp);
    
    /**
     * @see #rewriteIp(String, String, String)
     */
    default String rewriteIp(String sourceIp, String clientIp) {
        return this.rewriteIp(sourceIp, clientIp, null);
    }
    
    /**
     * 重写IP地址
     * 内网环境重写IP地址：修改网络号保留主机号
     * 
     * @param sourceIp 原始IP
     * @param clientIp 终端IP
     * @param defaultSourceIp 默认原始IP
     * 
     * @return 替换IP
     */
    String rewriteIp(String sourceIp, String clientIp, String defaultSourceIp);
    
}
