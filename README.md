# 桃夭

基于WebRTC实现信令服务，实现Mesh、MCU和SFU三种媒体通信架构，支持直播会议两种场景。
项目提供WebRTC服务信令，终端已有H5示例，其他终端需要自己实现。

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao|桃夭|桃之夭夭灼灼其华|
|taoyao-boot|基础模块|基础模块|
|taoyao-live|直播|直播、连麦|
|taoyao-test|测试|测试工具|
|taoyao-media|媒体|录制、视频（水印、美颜、AI识别）、音频（降噪、混音、变声）|
|taoyao-signal|信令|信令服务|
|taoyao-server|启动服务|启动服务|
|taoyao-meeting|会议|会议模式、广播模式、单人对讲|
|taoyao-webrtc|WebRTC模块|WebRTC模块|
|taoyao-webrtc-sfu|WebRTC SFU架构|SFU架构|
|taoyao-webrtc-mcu|WebRTC MCU架构|MCU架构|
|taoyao-webrtc-mesh|WebRTC MESH架构|MESH架构|
|taoyao-webrtc-jitsi|WebRTC协议簇jitsi实现|WebRTC协议簇jitsi实现|
|taoyao-webrtc-kurento|WebRTC协议簇kurento实现|WebRTC协议簇kurento实现|

> 终端负责推流，服务端负责处理媒体流，这些功能也可以在终端实现。主次码流没在终端实现，服务端实现可以有更多选择。

## 模块关系

```
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-server                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|            taoyao-live            |      taoyao-meeting       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-media                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|   taoyao-mcu   /   taoyao-sfu     |                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+      taoyao-mesh          +
|  taoyao-jitsi  /  taoyao-kurento  |                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-signal                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         taoyao-boot                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

## 内网穿透

请用公共STUN/TURN服务或者自行搭建coturn服务。

> 只有公网Mesh架构才需要真正的内网穿透

## 直播

终端推流到服务端，由服务端分流。

## 会议

Mesh架构声音视频控制部分功能均在终端实现，同时不会实现终端录制、美颜、AI识别、变声、混音等等功能。
MCU/SFU声音视频控制在服务端实现，如果没有终端订阅并且没有录制是不会对终端进行拉流。

### Mesh

流媒体点对点连接，不经过服务端。

### MCU

终端推流到服务端，由服务端分流并且混音。

### SFU

终端推流到服务端，由服务端分流没有混音。

## TODO

springdoc升级正式版本
springboot升级正式版本