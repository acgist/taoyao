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

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao-client-web|Web终端|Web终端|
|taoyao-client-media|媒体终端|媒体服务|
|taoyao-client-android|安卓终端|安卓终端|
|taoyao-signal-server|信令服务|终端信令控制|

## 功能

Web、信令已经完成大部分音视频功能，还有部分视频质量调整功能没有完成。

Android还在学习之中...

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

## TODO

* P2P
* 标识 -> ID
* 所有字段获取 -> get
* 优化JS错误回调 -> platform::error
* 反复测试推流拉流、拉人踢人、音频视频控制
* 24小时不关闭媒体/一秒一次推拉流十分钟测试/三十秒推拉流一小时测试

* AI、美颜、水印、滤镜
* 混音、降噪、回音消除、声音特效