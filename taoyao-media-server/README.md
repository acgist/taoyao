# 媒体

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

## 事件

```
mediasoup.observer.on("newworker", fn(worker));
worker.on("died", fn(error));
worker.observer.on("close", fn());
worker.observer.on("newrouter", fn(router));
worker.observer.on("newwebrtcserver", fn(router));
router.on(“workerclose”, fn());
router.observer.on(“close”, fn());
router.observer.on(“newtransport”, fn(transport));
router.observer.on(“newrtpobserver”, fn(rtpObserver));
transport.on("trace", fn(trace));
transport.on(“routerclose”, fn());
transport.on(“listenserverclose”, fn());
transport.on(“trace”, fn(trace));
webRtcTransport.on(“icestatechange”, fn(iceState))
webRtcTransport.on(“iceselectedtuplechange”, fn(iceSelectedTuple))
webRtcTransport.on(“dtlsstatechange”, fn(dtlsState))
webRtcTransport.on(“sctpstatechange”, fn(sctpState))
plainTransport.on(“tuple”, fn(tuple))
plainTransport.on(“rtcptuple”, fn(rtcpTuple))
plainTransport.on(“sctpstatechange”, fn(sctpState))
pipeTransport.on(“sctpstatechange”, fn(sctpState))
directTransport.on(“rtcp”, fn(rtcpPacket))
transport.observer.on(“close”, fn())
transport.observer.on(“newproducer”, fn(producer))
transport.observer.on(“newconsumer”, fn(consumer))
transport.observer.on(“newdataproducer”, fn(dataProducer))
transport.observer.on(“newdataconsumer”, fn(dataConsumer))
transport.observer.on(“trace”, fn(trace))
webRtcTransport.observer.on(“icestatechange”, fn(iceState))
webRtcTransport.observer.on(“iceselectedtuplechange”, fn(iceSelectedTuple))
webRtcTransport.observer.on(“dtlsstatechange”, fn(dtlsState))
webRtcTransport.observer.on(“sctpstatechange”, fn(sctpState))
plainTransport.observer.on(“tuple”, fn(tuple))
plainTransport.observer.on(“rtcptuple”, fn(rtcpTuple))
plainTransport.observer.on(“sctpstatechange”, fn(sctpState))
pipeTransport.observer.on(“sctpstatechange”, fn(sctpState))
```

## 安全

默认媒体服务只要暴露媒体`UDP`端口，信令接口不用暴露，所以使用简单鉴权。

## 动态调节码率

```
参考配置`mediaCodecs`
```

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

### 协议简介

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

## 其他常见WebRTC媒体服务

* [Jitsi](https://github.com/jitsi)
* [Janus](https://github.com/meetecho/janus-gateway/)
* [Licode](https://github.com/lynckia/licode)
* [Kurento](https://github.com/Kurento/kurento-media-server)
* [Medooze](https://github.com/medooze/media-server)
* [Mediasoup](https://github.com/versatica/mediasoup)
