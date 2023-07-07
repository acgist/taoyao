# Web终端

## Web终端

* [mediasoup-client源码](https://github.com/versatica/mediasoup-client)
* [mediasoup-client文档](https://mediasoup.org/documentation/v3/mediasoup-client)
* [mediasoup-client接口](https://mediasoup.org/documentation/v3/mediasoup-client/api)

## 终端媒体

终端页面组件需要提供`media`方法，同时挂载到终端的`proxy`属性下面。
媒体生成以后自动调用：

```
LocalClient.proxy.media(track, producer);
RemoteClient.proxy.media(track, consumer);
SessionClient.proxy.media(track);
```

## 终端列表

`Web`终端并未对整个终端列表以及状态进行维护，所以需要开发者自己实现。
