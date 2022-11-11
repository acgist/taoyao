# 桃夭

基于WebRTC实现信令服务，实现Mesh、MCU和SFU三种媒体通信架构，支持直播会议两种场景。

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao|桃夭|桃之夭夭灼灼其华|
|taoyao-nat|内网穿透|STUN/TURN|
|taoyao-boot|基础|启动模块|
|taoyao-live|直播|直播、连麦|
|taoyao-test|测试|测试工具|
|taoyao-media|媒体|录制、视频（美颜、AI识别）、音频（混音、变声、降噪）|
|taoyao-signal|信令|信令服务|
|taoyao-server|服务|启动服务|
|taoyao-meeting|会议|会议模式、广播模式、单人对讲|
|taoyao-webrtc|WebRTC模块|WebRTC模块|
|taoyao-webrtc-jni|WebRTC JNI|WebRTC本地接口|
|taoyao-webrtc-sfu|WebRTC SFU架构|SFU架构|
|taoyao-webrtc-mcu|WebRTC MCU架构|MCU架构|
|taoyao-webrtc-mesh|WebRTC MESH架构|MESH架构|

## STUN/TURN公共服务

```
stun:stun1.l.google.com:19302
stun:stun2.l.google.com:19302
stun:stun3.l.google.com:19302
stun:stun4.l.google.com:19302
stun:stun.stunprotocol.org:3478
```

## 信令

|功能|描述|
|:--|:--|
|注册|终端注册（同步信息）|
|关闭|终端关闭（注销）|
|心跳|终端心跳|
|进入会议|没有会议自动创建|
|离开会议|离开会议|
|关闭会议|关闭会议（所有人员离开）|
|邀请终端|会议邀请终端|
|踢出终端|会议踢出终端|
|推流|控制终端推流|
|暂停推流|控制终端暂停推流|
|订阅（分流）|控制终端暂停推流|
|暂停订阅（分流）|控制终端暂停推流|

## 直播

终端推流到服务端，由服务端分流。

## 会议

### Mesh

流媒体点对点连接，不经过服务端。

### MCU

终端推流到服务端，由服务端分流并且混音。

### SFU

终端推流到服务端，由服务端分流没有混音。

## 证书

```
keytool -genkeypair -keyalg RSA -dname "CN=localhost, OU=acgist, O=taoyao, L=GZ, ST=GD, C=CN" -alias taoyao -validity 3650 -ext ku:c=dig,keyE -ext eku=serverAuth -ext SAN=dns:localhost,ip:127.0.0.1 -keystore taoyao.jks -keypass 123456 -storepass 123456
```