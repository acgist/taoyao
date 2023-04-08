# 桃夭

桃夭是套基于`Mediasoup`开发的`WebRTC`音视频信令服务，可以非常方便的扩展信令接入更多智能终端。

<p align="center">
    <img alt="Java" src="https://img.shields.io/badge/dynamic/xml?style=flat-square&label=Java&color=blueviolet&url=https://raw.githubusercontent.com/acgist/taoyao/master/taoyao-signal-server/pom.xml&query=//*[local-name()=%27java.version%27]&cacheSeconds=3600" />
    <a target="_blank" href="https://starchart.cc/acgist/taoyao">
        <img alt="GitHub stars" src="https://img.shields.io/github/stars/acgist/taoyao?style=flat-square&label=Github%20stars&color=crimson" />
    </a>
    <img alt="Gitee stars" src="https://img.shields.io/badge/dynamic/json?style=flat-square&label=Gitee%20stars&color=crimson&url=https://gitee.com/api/v5/repos/acgist/taoyao&query=$.stargazers_count&cacheSeconds=3600" />
    <br />
    <img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/acgist/taoyao/build.yml?style=flat-square&branch=master" />
    <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/acgist/taoyao?style=flat-square&color=orange" />
    <img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/acgist/taoyao?style=flat-square&color=blue" />
    <img alt="GitHub" src="https://img.shields.io/github/license/acgist/taoyao?style=flat-square&color=blue" />
</p>

----

> 当前程序处于开发阶段，大部分功能没有实现，可以使用的功能也没有经过大量测试，建议不要用于生产。

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao-client-web|Web终端|Web终端|
|taoyao-client-media|媒体终端|媒体服务|
|taoyao-signal-server|信令服务|终端信令控制|
|taoyao-client-android|安卓终端|安卓终端|

### Web终端功能

|功能|是否支持|是否实现|描述|
|:--|:--|:--|:--|
|P2P|支持|实现|P2P监控模式|
|WebRTC|支持|实现|Web终端不能同时进入多个房间|

### 安卓终端功能

|功能|是否支持|是否实现|描述|
|:--|:--|:--|:--|
|P2P|支持|实现|P2P监控模式|
|WebRTC|支持|暂未实现|安卓终端支持同时进入多个房间|
|RTP|支持|暂未实现|支持房间RTP推流（不会拉流）|
|录像|支持|暂未实现||
|拍照|支持|暂未实现||
|变声|支持|暂未实现||
|水印|支持|暂未实现||
|美颜|支持|暂未实现||
|AI识别|支持|暂未实现||

## 证书

本地开发测试安装`docs/certs`中的`ca.crt`证书

## 部署

[部署文档](./docs/Deploy.md)

### 集群

信令服务支持下挂多个媒体服务，但是信令服务本身不具备分布式集群功能，如需实现给出以下两种实现建议：

#### 信令分区

将信令服务进行分区管理，分区不要直接管理终端，优先选择分区，然后选择信令服务。

#### 代理终端

将下级信令服务的终端全部使用代理终端注册到上级信令服务，上级信令服务代理终端处理信令时直接路由到下级路由服务，这样一级一级路由直到发送给真正的终端为止。
