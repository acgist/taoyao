# 桃夭

桃夭是套基于`Mediasoup`开发的`WebRTC`音视频信令服务

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao-client|终端示例|Web端终端示例|
|taoyao-media-server|媒体服务|Mediasoup媒体服务|
|taoyao-signal-server|信令服务|信令业务逻辑|

## 部署

[部署文档](./docs/Deploy.md)

### 流程

终端首先连接信令得到媒体服务配置，然后推送媒体流到媒体服务，最后通过信令操作各个终端媒体如何转发。

> 终端不能直接连接媒体服务操作其他终端媒体

## 媒体

* [mediasoup官网](https://mediasoup.org/)
* [mediasoup源码](https://github.com/versatica/mediasoup)
* [mediasoup文档](https://mediasoup.org/documentation/v3/mediasoup)
* [mediasoup接口](https://mediasoup.org/documentation/v3/mediasoup/api)

## Web终端

* [mediasoup-client源码](https://github.com/versatica/mediasoup-client)
* [mediasoup-client文档](https://mediasoup.org/documentation/v3/mediasoup-client)
* [mediasoup-client接口](https://mediasoup.org/documentation/v3/mediasoup-client/api)

## C++终端

* [libmediasoupclient源码](https://github.com/versatica/libmediasoupclient)
* [libmediasoupclient文档](https://mediasoup.org/documentation/v3/libmediasoupclient)
* [libmediasoupclient接口](https://mediasoup.org/documentation/v3/libmediasoupclient/api)

## TODO

* 录制（Recorder）
* 音频：降噪、混音、变声
* 视频：水印、美颜、AI识别
* 信令直传
* 信令服务集群
* 媒体交互式启动
* 一个信令服务多个媒体服务
