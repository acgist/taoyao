# 信令

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao|桃夭|桃之夭夭灼灼其华|
|taoyao-boot|基础|基础模块|
|taoyao-node|集群|集群模块|
|taoyao-media|媒体|媒体模块|
|taoyao-signal|信令|信令服务|
|taoyao-server|服务|启动服务|

### 直播

直播、连麦、监控、视频同看

### 会议

会议模式、广播模式、单人对讲

## 模块关系

```
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        taoyao-server                    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|         taoyao-media        |         Mediasoup         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       taoyao-signal                     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       taoyao-boot                       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

## WebRTC资料

https://www.cnblogs.com/ssyfj/p/14828185.html
https://www.cnblogs.com/ssyfj/p/14826516.html
https://www.cnblogs.com/ssyfj/p/14823861.html
https://www.cnblogs.com/ssyfj/p/14815266.html
https://www.cnblogs.com/ssyfj/p/14811253.html
https://www.cnblogs.com/ssyfj/p/14806678.html
https://www.cnblogs.com/ssyfj/p/14805040.html
https://www.cnblogs.com/ssyfj/p/14788663.html
https://www.cnblogs.com/ssyfj/p/14787012.html
https://www.cnblogs.com/ssyfj/p/14783168.html
https://www.cnblogs.com/ssyfj/p/14781982.html
https://www.cnblogs.com/ssyfj/p/14778839.html

## Mediasoup资料

https://www.cnblogs.com/ssyfj/p/14855454.html
https://www.cnblogs.com/ssyfj/p/14851442.html
https://www.cnblogs.com/ssyfj/p/14850041.html
https://www.cnblogs.com/ssyfj/p/14847097.html
https://www.cnblogs.com/ssyfj/p/14843182.html
https://www.cnblogs.com/ssyfj/p/14843082.html
