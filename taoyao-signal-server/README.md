# 信令

## 模块

|模块|名称|描述|
|:--|:--|:--|
|taoyao|桃夭|桃之夭夭灼灼其华|
|taoyao-boot|基础|基础模块|
|taoyao-signal|信令|信令模块|
|taoyao-server|服务|启动模块|

## 信令格式

[信令格式](https://localhost:8888/protocol/list)

## 测试脚本

```
let socket = new WebSocket("wss://localhost:8888/websocket.signal");
socket.send('{"header":{"signal":"client::register","v":"1.0.0","id":"1"},"body":{"username":"taoyao","password":"taoyao","clientId":"taoyao"}}');
socket.send('{"header":{"signal":"client::heartbeat","v":"1.0.0","id":"1"},"body":{}}');
```
