package com.acgist.taoyao.signal.client.gb;

import java.time.LocalDateTime;
import java.util.List;

import javax.sip.address.SipURI;
import javax.sip.address.URI;

import com.acgist.taoyao.boot.service.GbURI;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Getter;
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
        @JacksonXmlProperty(localName = "SumNum")       private Integer            sumNum;
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
        @JacksonXmlProperty(localName = "Reason")       private String             reason;
        @JacksonXmlProperty(localName = "Encode")       private String             encode;
        @JacksonXmlProperty(localName = "Record")       private String             record;
        @JacksonXmlProperty(localName = "DeviceTime")   private String             deviceTime;
        @JacksonXmlProperty(localName = "Alarmstatus")  private AlarmstatusWrapper alarmstatus;
        // Catalog
        @JacksonXmlProperty(localName = "DeviceList")   private DeviceListWrapper deviceList;
    }

    @Getter
    @Setter
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
        @JacksonXmlProperty(localName = "DeviceID")   private String deviceId;   // 设备ID
        @JacksonXmlProperty(localName = "DutyStatus") private String dutyStatus; // 设备名称
    }

    @Getter
    @Setter
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
        @JacksonXmlProperty(localName = "DeviceID")     private String deviceId;     // 设备ID
        @JacksonXmlProperty(localName = "Name")         private String name;         // 设备名称
        @JacksonXmlProperty(localName = "Manufacturer") private String manufacturer; // 设备厂商
        @JacksonXmlProperty(localName = "Model")        private String model;        // 设备型号
        @JacksonXmlProperty(localName = "CivilCode")    private String civilCode;    // 行政区域
        @JacksonXmlProperty(localName = "Address")      private String address;      // 安装地址
        @JacksonXmlProperty(localName = "Parental")     private String parental;     // 是否有子设备
        @JacksonXmlProperty(localName = "ParentID")     private String parentId;     // 上级节点DeviceID：没有上级节点设置平台DeviceID
        @JacksonXmlProperty(localName = "RegisterWay")  private String registerWay;  // 注册方式
        @JacksonXmlProperty(localName = "Secrecy")      private String secrecy;      // 保密属性
        @JacksonXmlProperty(localName = "Status")       private String status;       // 设备在线状态：ON在线；OFF离线。
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
                (this.host.equals(sipUri.getHost())     && this.port.equals(sipUri.getPort())    ) ||
                (this.domainId.equals(sipUri.getHost()) && this.deviceId.equals(sipUri.getUser()))
            );
        } else {
            return false;
        }
    }

}
