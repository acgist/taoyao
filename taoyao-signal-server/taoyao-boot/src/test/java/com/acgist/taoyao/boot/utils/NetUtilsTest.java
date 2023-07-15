package com.acgist.taoyao.boot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.RewriteProperties;
import com.acgist.taoyao.boot.config.RewriteRuleProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetUtilsTest {

    private void init() {
        final RewriteRuleProperties rewriteRuleProperties1 = new RewriteRuleProperties();
        rewriteRuleProperties1.setNetwork("192.168.1.0");
        final RewriteRuleProperties rewriteRuleProperties10 = new RewriteRuleProperties();
        rewriteRuleProperties10.setNetwork("192.168.10.0");
        final RewriteProperties rewriteProperties = new RewriteProperties();
        rewriteProperties.setEnabled(true);
        rewriteProperties.setPrefix(24);
        rewriteProperties.setRule(List.of(rewriteRuleProperties1, rewriteRuleProperties10));
        NetUtils.init(rewriteProperties);
    }
    
    @Test
    public void testSubnetIp() {
        this.init();
        assertTrue(NetUtils.subnetIp("192.168.8.1", "192.168.8.100"));
        assertTrue(NetUtils.subnetIp("192.168.100.1", "192.168.100.100"));
        assertFalse(NetUtils.subnetIp("192.168.1.1", "192.168.8.100"));
        assertFalse(NetUtils.subnetIp("192.168.80.1", "192.168.8.100"));
        assertTrue(NetUtils.subnetIp("fe80::9ff9:2da9:9759:17e9", "fe80::9ff9:2da9:9759:17e9"));
        assertTrue(NetUtils.subnetIp("fe80::9ff9:2da9:9759:17ee", "fe80::9ff9:2da9:9759:17e9"));
        assertFalse(NetUtils.subnetIp("fe81::9ff9:2da9:9759:17e9", "fe80::9ff9:2da9:9759:17e9"));
        assertFalse(NetUtils.subnetIp("fe81::9ff9:2da9:9759:17ee", "fe80::9ff9:2da9:9759:17e9"));
    }
    
    @Test
    public void testRewriteIp() {
        this.init();
        assertNotEquals("192.168.1.0", NetUtils.rewriteIp("0.0.0.0", "192.168.1.1"));
        assertEquals("192.168.1.100", NetUtils.rewriteIp("192.168.8.100", "192.168.1.1"));
        assertEquals("192.168.10.100", NetUtils.rewriteIp("192.168.8.100", "192.168.10.1"));
    }

    @Test
    public void testCost() {
        this.init();
        long a = System.currentTimeMillis();
        for (int index = 0; index < 100000; index++) {
            assertTrue(NetUtils.subnetIp("192.168.100.1", "192.168.100.100"));
            assertFalse(NetUtils.subnetIp("192.168.1.1", "192.168.8.100"));
        }
        long z = System.currentTimeMillis();
        log.info("耗时：{}", z - a);
        a = System.currentTimeMillis();
        for (int index = 0; index < 100000; index++) {
            assertEquals("192.168.1.100", NetUtils.rewriteIp("192.168.8.100", "192.168.1.1"));
            assertEquals("192.168.10.100", NetUtils.rewriteIp("192.168.8.100", "192.168.10.1"));
        }
        z = System.currentTimeMillis();
        log.info("耗时：{}", z - a);
    }
    
}
