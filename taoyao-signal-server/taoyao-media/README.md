# 媒体

# WebRTC

## WebRTC协议栈

```
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          HTTPS / WSS          |                   |    SCTP   |  SRTP / SRTCP   |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+  ICE / SDP / SIP  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|              TLS              |                   |                             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+    DTLS   +-+-+-+-+-+-+-+-+-+
|           HTTP / WS           | NAT / STUN / TURN |           |   RTP / RTCP    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|              TCP              |                     UDP                         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                 IPv4 / IPv6                                     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

## 协议简介

* 会话通道：ICE/SIP/SDP
* 媒体通道：RTP/RTCP/SRTP/SRTCP
* RTP：实时传输协议（音频视频）
* RTCP：RTP传输控制协议（监控数据传输质量并给予数据发送方反馈）
* SCTP：流控制传输协议（自定义的应用数据传输）
* RTMP：实时消息传送协议
* RTSP：可以控制媒体（点播）

### ICE/SDP/SIP

ICE信息的描述格式通常采用标准的SDP，其全称为Session Description Protocol，即会话描述协议。<br />
SDP只是一种信息格式的描述标准，不属于传输协议，但是可以被其他传输协议用来交换必要的信息，例如：SIP、RTSP等等。