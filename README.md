# 桃夭

基于WebRTC实现信令服务，实现Mesh、MCU和SFU三种媒体通信架构，支持直播会议两种场景。

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao|桃夭|桃之夭夭灼灼其华|
|taoyao-nat|内网穿透|STUN/TURN暂不实现（公共服务或者搭建coturn服务）|
|taoyao-live|直播|直播、连麦|
|taoyao-model|模型|数据模型|
|taoyao-media|媒体|录制、视频（美颜、AI识别）、音频（混音、变声）|
|taoyao-client|终端|帐号、摄像头|
|taoyao-signal|信令|信令服务|
|taoyao-server|服务|启动服务|
|taoyao-meeting|会议|会议模式、广播模式、单人对讲|
|taoyao-webrtc|WebRTC模块||
|taoyao-webrtc-sfu|WebRTC SFU架构实现||
|taoyao-webrtc-mcu|WebRTC MCU架构实现||
|taoyao-webrtc-mesh|WebRTC MESH架构实现||
|taoyao-webrtc-native|WebRTC底层实现|MCU/SFU底层媒体服务|

## STUN/TURN公共服务

```
stun:stun1.l.google.com:19302
stun:stun2.l.google.com:19302
stun:stun3.l.google.com:19302
stun:stun4.l.google.com:19302
stun:stun.stunprotocol.org:3478
```

## 终端

帐号可以管理媒体，摄像头只能被动管理。

### 功能

|功能|场景|描述|帐号|摄像头|
注册
注销
心跳
推流
拉流
邀请
踢出
绑定设备
解绑设备
进入会议：没有自动创建
关闭会议：
订阅
取消订阅
暂停推流
恢复推流
掉线重连

### 信息

IP
MAC
信号
电量
通话状态
录制状态

## 直播

## 会议

## 