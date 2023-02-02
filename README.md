# 桃夭

桃夭是套`WebRTC`信令服务，使用`Mediasoup`提供媒体服务，支持直播会议两种场景。

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao-client|终端|终端示例|
|taoyao-media-server|媒体|媒体服务|
|taoyao-signal-server|信令|信令服务|

### 流程

终端首先连接信令得到媒体服务配置，然后推送媒体流到媒体服务，最后通过信令操作各个终端媒体如何转发。

> 终端不能直接连接媒体服务操作其他终端媒体

## 媒体

* [mediasoup官网](https://mediasoup.org/)
* [mediasoup源码](https://github.com/versatica/mediasoup)
* [mediasoup文档](https://mediasoup.org/documentation/v3/mediasoup)
* [mediasoup接口](https://mediasoup.org/documentation/v3/mediasoup/api)

## 终端

* [mediasoup-client源码](https://github.com/versatica/mediasoup-client)
* [mediasoup-client文档](https://mediasoup.org/documentation/v3/mediasoup-client)
* [mediasoup-client接口](https://mediasoup.org/documentation/v3/mediasoup-client/api)

## TODO

* 录制
* 音频：降噪、混音、变声
* 视频：水印、美颜、AI识别
