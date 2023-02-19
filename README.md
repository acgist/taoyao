# 桃夭

桃夭是套基于`Mediasoup`开发的`WebRTC`音视频信令服务

<p align="center">
    <img alt="Java" src="https://img.shields.io/badge/dynamic/xml?style=flat-square&label=Java&color=blueviolet&url=https://raw.githubusercontent.com/acgist/taoyao/master/taoyao-signal-server/pom.xml&query=//*[local-name()=%27java.version%27]&cacheSeconds=3600" />
    <a target="_blank" href="https://starchart.cc/acgist/taoyao">
        <img alt="GitHub stars" src="https://img.shields.io/github/stars/acgist/taoyao?style=flat-square&label=Github%20stars&color=crimson" />
    </a>
    <img alt="Gitee stars" src="https://img.shields.io/badge/dynamic/json?style=flat-square&label=Gitee%20stars&color=crimson&url=https://gitee.com/api/v5/repos/acgist/taoyao&query=$.stargazers_count&cacheSeconds=3600" />
    <br />
    <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/acgist/taoyao?style=flat-square&color=orange" />
    <img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/acgist/taoyao?style=flat-square&color=blue" />
    <img alt="GitHub" src="https://img.shields.io/github/license/acgist/taoyao?style=flat-square&color=blue" />
</p>

----

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao-client-web|终端示例|Web终端示例|
|taoyao-client-android|终端示例|安卓终端示例|
|taoyao-media-server|媒体服务|媒体服务|
|taoyao-signal-server|信令服务|信令服务|

> 注意：只有Web实现完成信令控制，桌面还有安卓仅仅实现媒体收发。

## 模式

监控模式、直播模式、会议模式、屏幕共享模式

## 部署

[部署文档](./docs/Deploy.md)

### 集群

信令服务支持下挂多个媒体服务，但是信令服务本身不具备分布式集群功能，一下给出两种实现建议：

#### 信令分区

将信令服务进行分区管理，分区不要直接管理终端，优先选择分区，然后选择信令服务。

#### 代理终端

将下级信令服务的终端全部使用代理终端注册到上级信令服务，上级信令服务代理终端处理信令时直接路由到下级路由服务，这样一级一级路由直到发送给真正的终端为止。

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
* P2P
* 信令直传
* 信令服务集群
* 安全处理：房间
* 媒体交互式启动
* 会议调整为房间
* 内外网/多网卡环境
* 一个信令服务多个媒体服务
* 反复测试推流拉流、拉人踢人、音频视频控制
