# 桃夭

基于WebRTC实现信令服务，实现Mesh、MCU和SFU三种媒体通信架构，支持直播会议两种场景。
项目提供WebRTC服务信令，终端已有H5示例，其他终端需要自己实现。

## 授权

开源公益免费，商用需要购买授权。

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao|桃夭|桃之夭夭灼灼其华|
|taoyao-boot|启动模块|基础模块|
|taoyao-live|直播|直播、连麦|
|taoyao-test|测试|测试工具|
|taoyao-media|媒体|录制、视频（美颜、AI识别）、音频（混音、变声、降噪）|
|taoyao-signal|信令|信令服务|
|taoyao-server|服务|启动服务|
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
|                        taoyao-signal                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-media                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|   taoyao-mcu   |   taoyao-sfu     |                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+      taoyao-mesh          +
|  taoyao-jitsi  |  taoyao-kurento  |                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-boot                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

## 内网穿透

请用公共STUN/TURN服务或者自行搭建coturn服务。

> 只有公网Mesh架构才需要真正的内网穿透

### STUN/TURN公共服务地址

```
stun:stun1.l.google.com:19302
stun:stun2.l.google.com:19302
stun:stun3.l.google.com:19302
stun:stun4.l.google.com:19302
stun:stun.stunprotocol.org:3478
```

## 信令

|功能|描述|标识|响应|
|:--|:--|:--|:--|
|注册|终端注册（同步信息）|||
|关闭|终端关闭（注销）|||
|心跳|终端心跳|||
|创建会议|创建会议||返回会议ID|
|进入会议|没有会议自动创建||返回会议终端同时广播进入消息|
|离开会议|离开会议||广播离开消息|
|关闭会议|关闭会议（踢出所有人员）||广播关闭消息|
|终端列表|||返回所有终端列表|
|会议终端列表|||返回所有会议终端列表|
|直播终端列表|||返回所有直播终端列表|
|邀请终端|会议邀请终端（主动/被动）||单播邀请|
|踢出终端|会议踢出终端||单播踢出|
|开启直播||||
|关闭直播||||
|发布|控制终端推流|||
|取消发布|控制终端暂停推流|||
|订阅|订阅终端媒体流|||
|取消订阅|取消订阅终端媒体流|||
|暂停媒体流|暂停终端媒体流分流（不关媒体流通道）|||
|恢复媒体流|恢复终端媒体流分流（不关媒体流通道）|||
|开启录像||||
|关闭录像||||
|终端状态||||
|单播消息|发送指定终端|||
|广播消息|广播排除自己的所有终端|||
|全员广播消息|广播包括自己的所有终端|||
|异常|异常信息|||

## 直播

终端推流到服务端，由服务端分流。

## 会议

### Mesh

流媒体点对点连接，不经过服务端。

> 录制、AI识别等等功能只能在终端实现。

### MCU

终端推流到服务端，由服务端分流并且混音。

### SFU

终端推流到服务端，由服务端分流没有混音。

## 证书

```
keytool -genkeypair -keyalg RSA -dname "CN=localhost, OU=acgist, O=taoyao, L=GZ, ST=GD, C=CN" -alias taoyao -validity 3650 -ext ku:c=dig,keyE -ext eku=serverAuth -ext SAN=dns:localhost,ip:127.0.0.1 -keystore taoyao.jks -keypass 123456 -storepass 123456
```
