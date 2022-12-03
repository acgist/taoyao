# 桃夭

桃夭是套`WebRTC`信令服务，综合`Mesh`、`MCU`和`SFU`三种媒体通信架构，支持直播会议两种场景。

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao|桃夭|桃之夭夭灼灼其华|
|taoyao-boot|基础|基础模块|
|taoyao-live|直播|直播、连麦、本地视频同看|
|taoyao-media|媒体|录制<br />音频（降噪、混音、变声）<br />视频（水印、美颜、AI识别）|
|taoyao-signal|信令|信令服务|
|taoyao-server|服务|启动服务|
|taoyao-meeting|会议|会议模式、广播模式、单人对讲|
|taoyao-webrtc|WebRTC|WebRTC模块|
|taoyao-webrtc-mesh|Mesh架构|Mesh架构|
|taoyao-webrtc-moon|Moon架构|Moon架构|
|taoyao-webrtc-kurento|kurento框架|WebRTC协议簇kurento实现|

## 模块关系

```
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-server                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|            taoyao-live            |      taoyao-meeting       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-media                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      taoyao-webrtc-moon           |                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+    taoyao-webrtc-mesh     +
|     taoyao-webrtc-kurento         |                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-signal                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         taoyao-boot                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

> 综合比较`jitsi`|`kurento`两个框架最后选择`kurento`框架作为基础框架

## 架构比较

### Mesh

流媒体点对点连接，不经过服务端。

#### 功能简介

* ~~直播~~
* ~~媒体：降噪、变声、美颜录制、等等~~
* 可能需要自己搭建`coturn`服务实现`STUN`/`TURN`功能
* 终端和终端之间各自建立一个独立媒体连接

### Moon

综合`MCU`/`SFU`两种架构，终端推流到服务端，由服务端处理后分流。

> 为什么叫`Moon`：因为这是古诗词中最美丽的意象

#### 功能简介

* 需要安装[KMS服务](./docs/Deploy.md#kmskurento-media-server)
* 提供混音、变声、美颜、录制等等媒体功能
* 终端推送给服务端最高质量媒体，再由服务端根据订阅终端按配置分流。
* 终端和服务器之间建立两个媒体连接，一个本地媒体，一个远程媒体。
