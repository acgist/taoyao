package com.acgist.taoyao.boot.service.impl;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.service.IpService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IpServiceImpl implements IpService {

    @Autowired
    private TaoyaoProperties taoyaoProperties;
    
    /**
     * 本机IP
     */
    private String localIp;
    /**
     * 本机环回IP
     */
    private String loopbackIp;
    
    @PostConstruct
    public void init() {
        try {
            final InetAddress localHost = InetAddress.getLocalHost();
            final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
            this.localIp = localHost.getHostAddress();
            this.loopbackIp = loopbackAddress.getHostAddress();
            log.info("本机IP：{}", this.localIp);
            log.info("环回IP：{}", this.loopbackIp);
        } catch (UnknownHostException e) {
            log.error("获取本机IP地址异常", e);
        }
    }
    
    @Override
    public boolean subnetIp(final String sourceIp, final String clientIp, final String defaultSourceIp) {
        try {
            InetAddress clientAddress = InetAddress.getByName(clientIp);
            // 通配地址或者本地地址替换本机IP
            if(this.localAnyOrLoopAddress(clientAddress)) {
                clientAddress = InetAddress.getByName(this.localIp);
            }
            InetAddress sourceAddress = InetAddress.getByName(sourceIp);
            // 通配地址或者本地换回地址替换默认地址
            if(StringUtils.isNotEmpty(defaultSourceIp) && this.localAnyOrLoopAddress(sourceAddress)) {
                sourceAddress = InetAddress.getByName(defaultSourceIp);
            }
            if(this.localAddress(sourceAddress) && this.localAddress(clientAddress)) {
                final byte[] sourceBytes = sourceAddress.getAddress();
                final byte[] clientBytes = clientAddress.getAddress();
                final int length = (sourceBytes.length & clientBytes.length) * Byte.SIZE;
                final BitSet sourceBit = BitSet.valueOf(sourceBytes);
                final BitSet clientBit = BitSet.valueOf(clientBytes);
                sourceBit.set(this.taoyaoProperties.getIpMask(), length, true);
                clientBit.set(this.taoyaoProperties.getIpMask(), length, true);
                final BigInteger source = new BigInteger(sourceBit.toByteArray());
                final BigInteger client = new BigInteger(clientBit.toByteArray());
                return source.equals(client);
            }
        } catch (Exception e) {
            log.error("IP地址转换异常：{}-{}", sourceIp, clientIp, e);
        }
        return true;
    }
    
    @Override
    public String rewriteIp(final String sourceIp, final String clientIp, final String defaultSourceIp) {
        try {
            InetAddress clientAddress = InetAddress.getByName(clientIp);
            // 通配地址或者本地地址替换本机IP
            if(this.localAnyOrLoopAddress(clientAddress)) {
                clientAddress = InetAddress.getByName(this.localIp);
            }
            InetAddress sourceAddress = InetAddress.getByName(sourceIp);
            // 通配地址或者本地换回地址替换默认地址
            if(this.localAnyOrLoopAddress(sourceAddress) && StringUtils.isNotEmpty(defaultSourceIp)) {
                sourceAddress = InetAddress.getByName(defaultSourceIp);
            }
            if(this.localAddress(sourceAddress) && this.localAddress(clientAddress)) {
                final byte[] sourceBytes = sourceAddress.getAddress();
                final byte[] clientBytes = clientAddress.getAddress();
                final int length = (sourceBytes.length & clientBytes.length) * Byte.SIZE;
                final BitSet sourceBit = BitSet.valueOf(sourceBytes);
                final BitSet clientBit = BitSet.valueOf(clientBytes);
                // 替换网络号保留主机号
                for (int index = 0; index < this.taoyaoProperties.getIpMask(); index++) {
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
        } catch (Exception e) {
            log.error("IP地址转换异常：{}-{}", sourceIp, clientIp, e);
        }
        return sourceIp;
    }

    /**
     * @param inetAddress IP地址
     * 
     * @return 通配或者换回地址
     */
    private boolean localAnyOrLoopAddress(InetAddress inetAddress) {
        return
            inetAddress.isAnyLocalAddress() ||
            inetAddress.isLoopbackAddress();
    }
    
    /**
     * @param inetAddress IP地址
     * 
     * @return 本地IP
     */
    private boolean localAddress(InetAddress inetAddress) {
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
