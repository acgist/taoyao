# 信令

## 信令格式

[信令格式](https://localhost:8888/protocol/list)

## 测试

```
let socket = new WebSocket("wss://localhost:8888/websocket.signal");
socket.send('{"header":{"signal":"client::register","v":"1.0.0","id":"1"},"body":{"username":"taoyao","password":"taoyao","clientId":"taoyao"}}');
socket.send('{"header":{"signal":"client::heartbeat","v":"1.0.0","id":"1"},"body":{}}');
```
