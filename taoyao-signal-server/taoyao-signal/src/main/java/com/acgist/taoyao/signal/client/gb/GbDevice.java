package com.acgist.taoyao.signal.client.gb;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import javax.sip.address.SipURI;
import javax.sip.address.URI;

import com.acgist.taoyao.boot.service.GbURI;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class GbDevice implements GbURI {

    @Getter
    @Setter
    @JacksonXmlRootElement(localName = "Query")
    public static final class Query {
        @JacksonXmlProperty(localName = "CmdType")  private String cmdType;
        @JacksonXmlProperty(localName = "SN")       private String sn;
        @JacksonXmlProperty(localName = "DeviceID") private String deviceId;
    }

    @Getter
    @Setter
    @JacksonXmlRootElement(localName = "Notify")
    public static final class Notify {
        @JacksonXmlProperty(localName = "CmdType")  private String cmdType;
        @JacksonXmlProperty(localName = "SN")       private String sn;
        @JacksonXmlProperty(localName = "DeviceID") private String deviceId;
        @JacksonXmlProperty(localName = "Status")   private String status;
    }

    @Getter
    @Setter
    @JacksonXmlRootElement(localName = "Response")
    public static final class Message {
        @JacksonXmlProperty(localName = "CmdType")      private String             cmdType;
        @JacksonXmlProperty(localName = "SN")           private String             sn;
        @JacksonXmlProperty(localName = "DeviceID")     private String             deviceId;
        @JacksonXmlProperty(localName = "Status")       private String             status;
        @JacksonXmlProperty(localName = "Result")       private String             result;
        // DeviceInfo
        @JacksonXmlProperty(localName = "DeviceName")   private String             deviceName;
        @JacksonXmlProperty(localName = "Manufacturer") private String             manufacturer;
        @JacksonXmlProperty(localName = "Model")        private String             model;
        @JacksonXmlProperty(localName = "Firmware")     private String             firmware;
        @JacksonXmlProperty(localName = "Channel")      private Integer            channel;
        // DeviceStatus
        @JacksonXmlProperty(localName = "Online")       private String             online;
        @JacksonXmlProperty(localName = "Encode")       private String             encode;
        @JacksonXmlProperty(localName = "Record")       private String             record;
        @JacksonXmlProperty(localName = "DeviceTime")   private String             deviceTime;
        @JacksonXmlProperty(localName = "Alarmstatus")  private AlarmstatusWrapper alarmstatus;
        // Catalog
        @JacksonXmlProperty(localName = "SumNum")       private Integer            sumNum;
        @JacksonXmlProperty(localName = "DeviceList")   private DeviceListWrapper  deviceList;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class AlarmstatusWrapper {
        @JacksonXmlProperty(localName = "Num", isAttribute = true)
        private Integer num;
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "Item")
        private List<Alarmstatus> alarmstatusList;
    }

    @Getter
    @Setter
    public static final class Alarmstatus {
        @JacksonXmlProperty(localName = "DeviceID")   private String deviceId;
        @JacksonXmlProperty(localName = "DutyStatus") private String dutyStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class DeviceListWrapper {
        @JacksonXmlProperty(localName = "Num", isAttribute = true)
        private Integer num;
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "Item")
        private List<Device> deviceList;
    }

    @Getter
    @Setter
    public static final class Device {
        @JacksonXmlProperty(localName = "DeviceID")     private String deviceId;
        @JacksonXmlProperty(localName = "Name")         private String name;
        @JacksonXmlProperty(localName = "Manufacturer") private String manufacturer;
        @JacksonXmlProperty(localName = "Model")        private String model;
        @JacksonXmlProperty(localName = "CivilCode")    private String civilCode;
        @JacksonXmlProperty(localName = "Address")      private String address;
        @JacksonXmlProperty(localName = "Parental")     private String parental;
        @JacksonXmlProperty(localName = "ParentID")     private String parentId;
        @JacksonXmlProperty(localName = "RegisterWay")  private String registerWay;
        @JacksonXmlProperty(localName = "Secrecy")      private String secrecy;
        @JacksonXmlProperty(localName = "Status")       private String status;
    }

    protected String  transport;
    protected String  host;
    protected Integer port;
    protected String  domainId;
    protected String  deviceId;
    protected String  username;
    protected String  password;
    protected LocalDateTime lastActiveTime;

    public boolean equals(URI uri) {
        if (uri instanceof SipURI sipUri) {
            return (
                this.deviceId.equals(sipUri.getUser()) ||
                (
                    this.host.equals(sipUri.getHost()) &&
                    this.port.equals(sipUri.getPort())
                )
            );
        } else {
            return false;
        }
    }

    public boolean checkActiveTime(LocalDateTime now, long timeout) {
        return this.lastActiveTime != null && Duration.between(this.lastActiveTime, now).getSeconds() >= timeout;
    }

}
