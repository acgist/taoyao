package com.acgist.taoyao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.annotation.TaoyaoTest;
import com.acgist.taoyao.boot.service.IpService;
import com.acgist.taoyao.main.TaoyaoApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TaoyaoTest(classes = TaoyaoApplication.class)
public class IpServiceTest {

    @Autowired
    private IpService ipService;

    @Test
    public void testDomain() throws UnknownHostException {
        InetAddress byName = InetAddress.getByName("www.acgist.com");
        System.out.println(byName);
        System.out.println(byName.getHostAddress());
        System.out.println(byName.getHostName());
        System.out.println(byName.isAnyLocalAddress());
        System.out.println(byName.isLoopbackAddress());
        System.out.println(byName.isLinkLocalAddress());
        System.out.println(byName.isMulticastAddress());
        System.out.println(byName.isSiteLocalAddress());
    }
    
    @Test
    public void testSubnetIp() {
        assertTrue(this.ipService.subnetIp("192.168.8.1", "192.168.8.100"));
        assertTrue(this.ipService.subnetIp("192.168.100.1", "192.168.100.100"));
        assertFalse(this.ipService.subnetIp("192.168.1.1", "192.168.8.100"));
        assertFalse(this.ipService.subnetIp("192.168.80.1", "192.168.8.100"));
        assertTrue(this.ipService.subnetIp("fe80::9ff9:2da9:9759:17e9", "fe80::9ff9:2da9:9759:17e9"));
        assertTrue(this.ipService.subnetIp("fe80::9ff9:2da9:9759:17ee", "fe80::9ff9:2da9:9759:17e9"));
        assertFalse(this.ipService.subnetIp("fe81::9ff9:2da9:9759:17e9", "fe80::9ff9:2da9:9759:17e9"));
        assertFalse(this.ipService.subnetIp("fe81::9ff9:2da9:9759:17ee", "fe80::9ff9:2da9:9759:17e9"));
    }
    
    @Test
    public void testRewriteIp() {
        assertEquals("192.168.1.0", this.ipService.rewriteIp("0.0.0.0", "192.168.1.1"));
        assertEquals("192.168.1.10", this.ipService.rewriteIp("0.0.0.0", "192.168.1.1", "192.168.8.10"));
        assertEquals("192.168.1.100", this.ipService.rewriteIp("192.168.8.100", "192.168.1.1"));
        assertEquals("192.168.10.100", this.ipService.rewriteIp("192.168.8.100", "192.168.10.1"));
    }

    @Test
    public void testCost() {
        long a = System.currentTimeMillis();
        for (int index = 0; index < 100000; index++) {
            assertTrue(this.ipService.subnetIp("192.168.100.1", "192.168.100.100"));
            assertFalse(this.ipService.subnetIp("192.168.1.1", "192.168.8.100"));
        }
        long z = System.currentTimeMillis();
        log.info("耗时：{}", z - a);
        a = System.currentTimeMillis();
        for (int index = 0; index < 100000; index++) {
            assertEquals("192.168.1.100", this.ipService.rewriteIp("192.168.8.100", "192.168.1.1"));
            assertEquals("192.168.10.100", this.ipService.rewriteIp("192.168.8.100", "192.168.10.1"));
        }
        z = System.currentTimeMillis();
        log.info("耗时：{}", z - a);
    }
    
}
