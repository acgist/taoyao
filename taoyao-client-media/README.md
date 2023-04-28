# 媒体终端

只要负责媒体处理，不要添加任何业务逻辑，所有业务逻辑都由[taoyao-signal-server](../taoyao-signal-server)处理。

## 媒体

* [mediasoup官网](https://mediasoup.org/)
* [mediasoup源码](https://github.com/versatica/mediasoup)
* [mediasoup文档](https://mediasoup.org/documentation/v3/mediasoup)
* [mediasoup接口](https://mediasoup.org/documentation/v3/mediasoup/api)

## 使用

```
sudo npm install
```

## Mediasoup

```
# 编译：默认不用手动编译
make
make -C worker
```

## 节点配置

需要保证`src/Config.js`中的`clientId`和`ecosystem.config.json`中的`name`保持一致，否者重启和关闭信令无效。

## 动态调节码率

```
参考配置`mediaCodecs`
```

## WebRTC协议栈

```
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          HTTPS / WSS          |                   |    SCTP   |  SRTP / SRTCP   |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+     ICE / SDP     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|              TLS              |                   |                             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+    DTLS   +-+-+-+-+-+-+-+-+-+
|           HTTP / WS           | NAT / STUN / TURN |           |   RTP / RTCP    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|              TCP              |                     UDP                         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                 IPv4 / IPv6                                     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 协议简介

* 会话通道：ICE/SDP
* 媒体通道：RTP/RTCP/SRTP/SRTCP
* SDP：会话描述协议（只是信息格式描述标准不是传输协议）
* ICE：交互式连接建立（使用标准SDP描述）
* RTP：实时传输协议
* RTCP：RTP控制协议（监控数据传输质量提供反馈）
* SCTP：流控制传输协议
* RTSP：实时流传输协议（依赖RTP协议实时性好适合视频聊天视频监控）
* RTMP：实时消息传输协议

## 其他常见WebRTC媒体服务

* [Jitsi](https://github.com/jitsi)
* [Janus](https://github.com/meetecho/janus-gateway/)
* [Licode](https://github.com/lynckia/licode)
* [Kurento](https://github.com/Kurento/kurento-media-server)
* [Medooze](https://github.com/medooze/media-server)
* [Mediasoup](https://github.com/versatica/mediasoup)

## RTP裸流

媒体服务主要使用`WebRTC`协议，同时支持接入`RTP`裸流，可以参考[RtpTest.java](../taoyao-signal-server/taoyao-server/src/test/java/com/acgist/taoyao/rtp/RtpTest.java)配合`ffmpeg`使用`RTP`推拉流，具体代码需要自行实现。

## 协议

* https://www.ortc.org
* https://www.webrtc.org
