package com.acgist.taoyao.boot.utils;

import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.config.RewriteProperties;
import com.acgist.taoyao.boot.config.RewriteRuleProperties;

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
    private static String localIP;
    /**
     * 环回IP
     */
    private static String loopbackIP;
    /**
     * 地址重写
     */
    private static RewriteProperties rewriteProperties;
    
    /**
     * 加载配置
     */
    public static final void init(RewriteProperties rewriteProperties) {
        try {
            NetUtils.rewriteProperties  = rewriteProperties;
            final InetAddress localHost = InetAddress.getLocalHost();
            final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
            NetUtils.localIP    = localHost.getHostAddress();
            NetUtils.loopbackIP = loopbackAddress.getHostAddress();
            log.info("本机IP：{}", NetUtils.localIP);
            log.info("环回IP：{}", NetUtils.loopbackIP);
        } catch (UnknownHostException e) {
            log.error("加载网络配置异常", e);
        }
    }
    
    /**
     * @return 本机IP
     */
    public static final String localIP() {
        return NetUtils.localIP;
    }
    
    /**
     * @return 环回IP
     */
    public static final String loopbackIP() {
        return NetUtils.loopbackIP;
    }
    
    /**
     * 内网是否相同网段
     * 
     * @param sourceIP 原始IP
     * @param clientIP 终端IP
     * 
     * @return 是否匹配
     */
    public static final boolean subnetIP(final String sourceIP, final String clientIP) {
        try {
            final InetAddress sourceAddress = NetUtils.realAddress(sourceIP);
            final InetAddress clientAddress = NetUtils.realAddress(clientIP);
            final boolean sourceLocal = NetUtils.localAddress(sourceAddress);
            final boolean clientLocal = NetUtils.localAddress(clientAddress);
            if(sourceLocal && clientLocal) {
                final byte[] sourceBytes = sourceAddress.getAddress();
                final byte[] clientBytes = clientAddress.getAddress();
                final int length = (sourceBytes.length & clientBytes.length) * Byte.SIZE;
                final BitSet sourceBit = BitSet.valueOf(sourceBytes);
                final BitSet clientBit = BitSet.valueOf(clientBytes);
                sourceBit.set(NetUtils.rewriteProperties.getPrefix(), length, true);
                clientBit.set(NetUtils.rewriteProperties.getPrefix(), length, true);
                final BigInteger source = new BigInteger(sourceBit.toByteArray());
                final BigInteger client = new BigInteger(clientBit.toByteArray());
                return source.equals(client);
            }
        } catch (UnknownHostException e) {
            log.error("IP地址转换异常：{} - {}", sourceIP, clientIP, e);
        }
        return false;
    }
    
    /**
     * 重写地址
     * 
     * @param sourceIP 原始IP
     * @param clientIP 终端IP
     * 
     * @return 重写IP
     */
    public static final String rewriteIP(final String sourceIP, final String clientIP) {
        if(Boolean.FALSE.equals(NetUtils.rewriteProperties.getEnabled())) {
            return sourceIP;
        }
        try {
            final InetAddress sourceAddress = NetUtils.realAddress(sourceIP);
            final InetAddress clientAddress = NetUtils.realAddress(clientIP);
            final boolean sourceLocal = NetUtils.localAddress(sourceAddress);
            final boolean clientLocal = NetUtils.localAddress(clientAddress);
            // 内网服务 && 内网设备 => 重写服务地址
            if(sourceLocal && clientLocal) {
                if(NetUtils.subnetIP(sourceIP, clientIP)) {
                    return sourceIP;
                }
                final RewriteRuleProperties rule = NetUtils.rewriteProperties.getRule().stream()
                    .filter(v -> NetUtils.subnetIP(v.getNetwork(), clientIP))
                    .findFirst()
                    .orElse(null);
                if(rule == null) {
                    return sourceIP;
                }
                if(StringUtils.isNotEmpty(rule.getInnerHost())) {
                    return rule.getInnerHost();
                }
                log.debug("地址重写：{} - {} - {}", sourceIP, clientIP, rule.getNetwork());
                // 地址 = 网络号 + 主机号
                final byte[] sourceBytes = sourceAddress.getAddress();
                final byte[] clientBytes = clientAddress.getAddress();
                final int length = (sourceBytes.length & clientBytes.length) * Byte.SIZE;
                final BitSet sourceBit = BitSet.valueOf(sourceBytes);
                final BitSet clientBit = BitSet.valueOf(clientBytes);
                // 替换网络号保留主机号
                for (int index = 0; index < NetUtils.rewriteProperties.getPrefix(); index++) {
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
            // 内网服务 && 公网设备 => 公网服务地址
            if(sourceLocal && !clientLocal) {
                final RewriteRuleProperties rule = NetUtils.rewriteProperties.getRule().stream()
                    .filter(v -> NetUtils.subnetIP(v.getNetwork(), sourceIP))
                    .findFirst()
                    .orElse(null);
                if(rule == null) {
                    return sourceIP;
                }
                if(StringUtils.isNotEmpty(rule.getOuterHost())) {
                    return rule.getOuterHost();
                }
            }
            // 公网服务 && 内网设备 => 内网服务地址
            if(!sourceLocal && clientLocal) {
                final RewriteRuleProperties rule = NetUtils.rewriteProperties.getRule().stream()
                    .filter(v -> NetUtils.subnetIP(v.getNetwork(), clientIP))
                    .findFirst()
                    .orElse(null);
                if(rule == null) {
                    return sourceIP;
                }
                if(StringUtils.isNotEmpty(rule.getInnerHost())) {
                    return rule.getInnerHost();
                }
            }
        } catch (UnknownHostException e) {
            log.error("地址重写异常：{} - {}", sourceIP, clientIP, e);
        }
        return sourceIP;
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
            return InetAddress.getByName(NetUtils.localIP);
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
    
    /**
     * 扫描端口
     * 
     * @param min 最小端口
     * @param max 最大端口
     * 
     * @return 端口
     */
    public static final int scanPort(int min, int max) {
        for (int port = min; port < max; port++) {
            try (final DatagramSocket socket = new DatagramSocket(port)) {
                socket.disconnect();
                return port;
            } catch (SocketException e) {
                // 忽略
            }
        }
        return 0;
    }
    
}
