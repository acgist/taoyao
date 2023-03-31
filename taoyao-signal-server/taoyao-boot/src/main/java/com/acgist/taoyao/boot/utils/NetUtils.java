package com.acgist.taoyao.boot.utils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.config.IpRewriteProperties;
import com.acgist.taoyao.boot.config.IpRewriteRuleProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 网络工具
 * 
 * @author acgist
 */
@Slf4j
public final class NetUtils {
    
    private NetUtils() {
    }
    
    /**
     * 本机IP
     */
    private static String localIp;
    /**
     * 环回IP
     */
    private static String loopbackIp;
    /**
     * 地址重写
     */
    private static IpRewriteProperties ipRewriteProperties;
    
    /**
     * 加载配置
     */
    public static final void init(IpRewriteProperties ipRewriteProperties) {
        try {
            NetUtils.ipRewriteProperties = ipRewriteProperties;
            final InetAddress localHost = InetAddress.getLocalHost();
            final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
            NetUtils.localIp = localHost.getHostAddress();
            NetUtils.loopbackIp = loopbackAddress.getHostAddress();
            log.info("本机IP：{}", NetUtils.localIp);
            log.info("环回IP：{}", NetUtils.loopbackIp);
        } catch (UnknownHostException e) {
            log.error("加载网络配置异常", e);
        }
    }
    
    /**
     * @return 本机IP
     */
    public static final String localIp() {
        return NetUtils.localIp;
    }
    
    /**
     * @return 环回IP
     */
    public static final String loopbackIp() {
        return NetUtils.loopbackIp;
    }
    
    /**
     * 内网是否相同网段
     * 
     * @param sourceIp 原始IP
     * @param clientIp 终端IP
     * 
     * @return 是否匹配
     */
    public static final boolean subnetIp(final String sourceIp, final String clientIp) {
        try {
            final InetAddress sourceAddress = NetUtils.realAddress(sourceIp);
            final InetAddress clientAddress = NetUtils.realAddress(clientIp);
            if(NetUtils.localAddress(sourceAddress) && NetUtils.localAddress(clientAddress)) {
                final byte[] sourceBytes = sourceAddress.getAddress();
                final byte[] clientBytes = clientAddress.getAddress();
                final int length = (sourceBytes.length & clientBytes.length) * Byte.SIZE;
                final BitSet sourceBit = BitSet.valueOf(sourceBytes);
                final BitSet clientBit = BitSet.valueOf(clientBytes);
                sourceBit.set(NetUtils.ipRewriteProperties.getPrefix(), length, true);
                clientBit.set(NetUtils.ipRewriteProperties.getPrefix(), length, true);
                final BigInteger source = new BigInteger(sourceBit.toByteArray());
                final BigInteger client = new BigInteger(clientBit.toByteArray());
                return source.equals(client);
            }
        } catch (UnknownHostException e) {
            log.error("IP地址转换异常：{}-{}", sourceIp, clientIp, e);
        }
        return true;
    }
    
    /**
     * 重写地址
     * 
     * @param sourceIp 原始IP
     * @param clientIp 终端IP
     * 
     * @return 替换IP
     */
    public static final String rewriteIp(final String sourceIp, final String clientIp) {
        if(Boolean.FALSE.equals(NetUtils.ipRewriteProperties.getEnabled())) {
            return sourceIp;
        }
        log.debug("重写地址：{} - {}", sourceIp, clientIp);
        try {
            final InetAddress sourceAddress = NetUtils.realAddress(sourceIp);
            final InetAddress clientAddress = NetUtils.realAddress(clientIp);
            if(NetUtils.localAddress(sourceAddress) && NetUtils.localAddress(clientAddress)) {
                final IpRewriteRuleProperties rule = NetUtils.ipRewriteProperties.getRule().stream()
                    .filter(v -> NetUtils.subnetIp(v.getNetwork(), clientIp))
                    .findFirst()
                    .orElse(null);
                if(rule == null) {
                    return sourceIp;
                }
                if(StringUtils.isNotEmpty(rule.getTargetHost())) {
                    return rule.getTargetHost();
                }
                final byte[] sourceBytes = sourceAddress.getAddress();
                final byte[] clientBytes = clientAddress.getAddress();
                final int length = (sourceBytes.length & clientBytes.length) * Byte.SIZE;
                final BitSet sourceBit = BitSet.valueOf(sourceBytes);
                final BitSet clientBit = BitSet.valueOf(clientBytes);
                // 替换网络号保留主机号
                for (int index = 0; index < NetUtils.ipRewriteProperties.getPrefix(); index++) {
                    sourceBit.set(index, clientBit.get(index));
                }
                final byte[] bytes = sourceBit.toByteArray();
                if(bytes.length < length) {
                    final byte[] fillBytes = new byte[length / Byte.SIZE];
                    System.arraycopy(bytes, 0, fillBytes, 0, bytes.length);
                    return InetAddress.getByAddress(fillBytes).getHostAddress();
                } else {
                    return InetAddress.getByAddress(bytes).getHostAddress();
                }
            }
        } catch (UnknownHostException e) {
            log.error("IP地址转换异常：{}-{}", sourceIp, clientIp, e);
        }
        return sourceIp;
    }
    
    /**
     * @param ip IP
     * 
     * @return 真实地址
     * 
     * @throws UnknownHostException 地址转换异常
     */
    public static final InetAddress realAddress(String ip) throws UnknownHostException {
        final InetAddress address = InetAddress.getByName(ip);
        if(NetUtils.localAnyOrLoopAddress(address)) {
            // 通配地址或者换回地址使用本机地址
            return InetAddress.getByName(NetUtils.localIp);
        }
        return address;
    }
    
    /**
     * @param inetAddress IP地址
     * 
     * @return 通配地址或者换回地址
     */
    public static final boolean localAnyOrLoopAddress(InetAddress inetAddress) {
        return
            // 通配地址：0.0.0.0
            inetAddress.isAnyLocalAddress() ||
            // 环回地址：127.0.0.1 | 0:0:0:0:0:0:0:1
            inetAddress.isLoopbackAddress();
    }
    
    /**
     * @param inetAddress IP地址
     * 
     * @return 本地IP
     */
    public static final boolean localAddress(InetAddress inetAddress) {
        return
            // 通配地址：0.0.0.0
            inetAddress.isAnyLocalAddress() ||
            // 环回地址：127.0.0.1 | 0:0:0:0:0:0:0:1
            inetAddress.isLoopbackAddress() ||
            // 链接地址：虚拟网卡
            inetAddress.isLinkLocalAddress() ||
            // 组播地址
            inetAddress.isMulticastAddress() ||
            // 本地地址：A/B/C类本地地址
            inetAddress.isSiteLocalAddress();
    }

}
