# 媒体

只要负责媒体处理，不要添加任何业务逻辑，所有业务逻辑都由[taoyao-signal](../taoyao-signal)处理。

## Mediasoup

```
# 模块
git submodule init
git submodule update
git submodule foreach
git submodule update --remote
git submodule update --init --recursive
git submodule add https://github.com/acgist/mediasoup.git taoyao-media-server/mediasoup
git submodule set-url taoyao-media-server/mediasoup https://github.com/acgist/mediasoup.git
git submodule set-branch --branch taoyao taoyao-media-server/mediasoup

# 编译

```

## 使用

```
sudo npm install
```

## 其他常见WebRTC媒体服务

* [Janus](https://github.com/meetecho/janus-gateway/)
* [Jitsi](https://github.com/jitsi)
* [Licode](https://github.com/lynckia/licode)
* [Kurento](https://github.com/Kurento/kurento-media-server)
* [Medooze](https://github.com/medooze/media-server)
* [Mediasoup](https://github.com/versatica/mediasoup)
