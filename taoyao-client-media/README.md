# 媒体终端

只要负责媒体处理，不要添加任何业务逻辑，所有业务逻辑都由[taoyao-signal-server](../taoyao-signal-server)处理。

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
