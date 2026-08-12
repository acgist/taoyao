package com.acgist.taoyao.signal.client.gb;

import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.GbProperties;
import com.acgist.taoyao.boot.config.GbProperties.Server;

class GbServerTest {

    @Test
    public void testJni() {
        GbMediaServer.loadJniLib("D:\\gitee\\taoyao\\taoyao-gb-media-server\\build\\Release\\taoyao-gb-media-server.dll");
        GbMediaServer.init();
        GbMediaServer.recv("1", "udp");
    }

    @Test
    public void testInit() throws ObjectInUseException, PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, TooManyListenersException, InterruptedException {
        final GbProperties gbProperties = new GbProperties();
        gbProperties.setHost("192.168.12.244");
        gbProperties.setJniLib("D:\\gitee\\taoyao\\taoyao-gb-media-server\\build\\Release\\taoyao-gb-media-server.dll");
        final GbServer server = new GbServer(gbProperties, null, null, null);
        server.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testRegister() throws ObjectInUseException, PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, TooManyListenersException, InterruptedException {
        final GbProperties gbProperties = new GbProperties();
        gbProperties.setHost("192.168.12.244");
        gbProperties.setJniLib("D:\\gitee\\taoyao\\taoyao-gb-media-server\\build\\Release\\taoyao-gb-media-server.dll");
        gbProperties.getServer().add(new Server("UDP", "192.168.8.183", 8116, "4101050000", "41010500002000000001", "admin", "admin123456"));
        final GbServer server = new GbServer(gbProperties, null, null, null);
        server.init();
        server.registerServer();
        Thread.sleep(5000);
        server.scheduled();
        Thread.sleep(Long.MAX_VALUE);
    }

}
