package com.acgist.taoyao.signal.client.gb;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.GbProperties;
import com.acgist.taoyao.boot.config.GbProperties.Upper;
import com.acgist.taoyao.signal.client.gb.GbDevice.Message;

class GbServerTest {

    @Test
    public void testInit() throws ObjectInUseException, PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, TooManyListenersException, InterruptedException {
        final GbServer server = new GbServer(new GbProperties(), null, null, null);
        server.init();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testRegister() throws ObjectInUseException, PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, TooManyListenersException, InterruptedException {
        final GbProperties gbProperties = new GbProperties();
        gbProperties.getUpper().add(new Upper("UDP", "192.168.8.183", 8116, "4101050000", "41010500002000000001", "admin", "admin123456"));
        final GbServer server = new GbServer(gbProperties, null, null, null);
        server.init();
        server.registerServer();
        Thread.sleep(5000);
        server.scheduled();
        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testXML() {
        final String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <Response>
            <CmdType>Catalog</CmdType>
            <SN>1234</SN>
            <DeviceID>34020000002020000001</DeviceID>
            
            <Status>OK</Status>
            <Result>OK</Result>

            <Manufacturer>TestCorp</Manufacturer>
            <Model>GB‑IPC‑V1</Model>
            <Firmware>V1.0.2</Firmware>
            <SerialNumber>SN202608060001</SerialNumber>

            <Online>ONLINE</Online>
            <Encode>ON</Encode>
            <Record>OFF</Record>
            <DeviceTime>2026‑08‑06T17:20:00</DeviceTime>
            <Alarmstatus Num="0"></Alarmstatus>

            <SumNum>4</SumNum>
            <DeviceList Num="4">
                <Item>
                    <DeviceID>34020000002020000001</DeviceID>
                    <Name>下级业务平台</Name>
                    <Manufacturer>MyPlatform</Manufacturer>
                    <Model>GB28181‑PLATFORM</Model>
                    <Owner>xxx</Owner>
                    <CivilCode>340200</CivilCode>
                    <Parental>1</Parental>
                    <ParentID>34020000002020000001</ParentID>
                    <SafetyWay>0</SafetyWay>
                    <RegisterWay>3</RegisterWay>
                    <Secrecy>0</Secrecy>
                    <Status>ON</Status>
                </Item>
                <Item>
                    <DeviceID>34020000002000000001</DeviceID>
                    <Name>NVR‑01</Name>
                    <Manufacturer>HIK</Manufacturer>
                    <Model>DS‑7808</Model>
                    <Owner>xxx</Owner>
                    <CivilCode>340200</CivilCode>
                    <Parental>1</Parental>
                    <ParentID>34020000002020000001</ParentID>
                    <SafetyWay>0</SafetyWay>
                    <RegisterWay>1</RegisterWay>
                    <Secrecy>0</Secrecy>
                    <Status>ON</Status>
                </Item>
                <Item>
                    <DeviceID>34020000001320000001</DeviceID>
                    <Name>通道‑1</Name>
                    <Manufacturer>HIK</Manufacturer>
                    <Model>IPC</Model>
                    <Owner>xxx</Owner>
                    <CivilCode>340200</CivilCode>
                    <Parental>0</Parental>
                    <ParentID>34020000002000000001</ParentID>
                    <SafetyWay>0</SafetyWay>
                    <RegisterWay>1</RegisterWay>
                    <Secrecy>0</Secrecy>
                    <Status>ON</Status>
                </Item>
                <Item>
                    <DeviceID>34020000001320000002</DeviceID>
                    <Name>通道‑2</Name>
                    <Manufacturer>HIK</Manufacturer>
                    <Model>IPC</Model>
                    <Owner>xxx</Owner>
                    <CivilCode>340200</CivilCode>
                    <Parental>0</Parental>
                    <ParentID>34020000002000000001</ParentID>
                    <SafetyWay>0</SafetyWay>
                    <RegisterWay>1</RegisterWay>
                    <Secrecy>0</Secrecy>
                    <Status>ON</Status>
                </Item>
            </DeviceList>
        </Response>
        """;
        final Message response = GbXML.message(xml);
        assertNotNull(response);
        final String out = GbXML.write(response);
        assertNotNull(out);
    }

}
