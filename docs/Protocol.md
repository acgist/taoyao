
## 信令格式

```
{
    "code"   : "状态编码",
    "message": "状态描述",
    "header": {
        "v"     : "消息版本",
        "id"    : "消息标识",
        "signal": "信令标识"
    },
    "body": {
        ...
    }
}
```

### 符号解释

```
-[消息类型]> 异步请求 | 单播消息
=[消息类型]> 同步请求：到达目标终端后沿原路返回
-[消息类型]) 全员广播：对所有的终端广播信令（排除自己）
+[消息类型]) 全员广播：对所有的终端广播信令（包含自己）
...：其他自定义的透传内容
```

> 消息类型可以省略表示和前面一致

### 终端告警信令（client::alarm）

```
# 消息主体
{
    "message" : "告警描述",
    "datetime": "告警时间（yyyyMMddHHmmss）"
}
# 数据流向
终端->信令服务
```

### 终端广播信令（client::broadcast）

没有指定终端类型时广播所有类型终端

```
# 消息主体
{
    "clientType": "终端类型（可选）"
    ...
}
# 数据流向
终端->信令服务-)终端
```

### 关闭终端信令（client::close）

信令连接断开以后执行，同时释放所有资源。
如果终端意外掉线，需要自己实现重连逻辑。

```
# 消息主体
{}
# 数据流向
终端=>信令服务
终端=[关闭终端]>信令服务-[终端下线])终端
终端-[连接断开]>信令服务-[终端下线])终端
```

### 终端配置信令（client::config）

终端应该在收到配置之后进行媒体操作

```
# 消息主体
{
    "media"   : "媒体配置（可选）",
    "webrtc"  : "WebRTC配置（可选）",
    "datetime": "日期时间（yyyyMMddHHmmss）"
}
# 数据流向
终端=[终端注册]>信令服务-[终端配置]>终端
```

### 终端心跳信令（client::heartbeat）

```
# 消息主体
{
    "latitude"       : 纬度,
    "longitude"      : 经度,
    "humidity"       : 湿度,
    "temperature"    : 温度,
    "signal"         : 信号强度（0~100）,
    "battery"        : 电池电量（0~100）,
    "alarming"       : 是否发生告警（true|false）,
    "charging"       : 是否正在充电（true|false）,
    "clientRecording": 是否正在录像（true|false）,
    "serverRecording": 是否正在录像（true|false）,
    "lastHeartbeat"  : "最后心跳时间",
    "status"         : {更多状态},
    "config"         : {更多配置}
}
# 数据流向
终端->信令服务->终端
```

### 终端列表信令（client::list）

没有选择终端类型时返回所有类型终端状态列表

```
# 消息主体
{
    "clientType": "终端类型（可选）"
}
[
    {
        "ip"             : "终端IP",
        "name"           : "终端名称",
        "clientId"       : "终端ID",
        "clientType"     : "终端类型",
        "latitude"       : 纬度,
        "longitude"      : 经度,
        "humidity"       : 湿度,
        "temperature"    : 温度,
        "signal"         : 信号强度（0~100）,
        "battery"        : 电池电量（0~100）,
        "alarming"       : 是否发生告警（true|false）,
        "charging"       : 是否正在充电（true|false）,
        "clientRecording": 是否正在录像（true|false）,
        "serverRecording": 是否正在录像（true|false）,
        "lastHeartbeat"  : "最后心跳时间",
        "status"         : {更多状态},
        "config"         : {更多配置}
    },
    ...
]
# 数据流向
终端=>信令服务
```

### 终端下线信令（client::offline）

```
# 消息主体
{
    "clientId": "下线终端ID"
}
# 数据流向
终端=[关闭终端]>信令服务-[终端下线])终端
终端-[连接断开]>信令服务-[终端下线])终端
```

### 终端上线信令（client::online）

```
# 消息主体
{
    "ip"             : "终端IP",
    "name"           : "终端名称",
    "clientId"       : "终端ID",
    "clientType"     : "终端类型",
    "latitude"       : 纬度,
    "longitude"      : 经度,
    "humidity"       : 湿度,
    "temperature"    : 温度,
    "signal"         : 信号强度（0~100）,
    "battery"        : 电池电量（0~100）,
    "alarming"       : 是否发生告警（true|false）,
    "charging"       : 是否正在充电（true|false）,
    "clientRecording": 是否正在录像（true|false）,
    "serverRecording": 是否正在录像（true|false）,
    "lastHeartbeat"  : "最后心跳时间",
    "status"         : {更多状态},
    "config"         : {更多配置}
}
# 数据流向
终端=[终端注册]>信令服务-)终端
```

### 重启终端信令（client::reboot）

```
# 消息主体
{}
# 数据流向
信令服务->终端
```

### 终端注册信令（client::register）

收到注册响应之后应该设置终端的终端索引

```
# 消息主体
{
    "username"       : "信令用户",
    "password"       : "信令密码",
    "name"           : "终端名称",
    "clientId"       : "终端ID",
    "clientType"     : "终端类型",
    "latitude"       : 纬度,
    "longitude"      : 经度,
    "humidity"       : 湿度,
    "temperature"    : 温度,
    "signal"         : 信号强度（0~100）,
    "battery"        : 电池电量（0~100）,
    "alarming"       : 是否发生告警（true|false）,
    "charging"       : 是否正在充电（true|false）,
    "clientRecording": 是否正在录像（true|false）,
    "serverRecording": 是否正在录像（true|false）,
    "lastHeartbeat"  : "最后心跳时间",
    "status"         : {更多状态},
    "config"         : {更多配置}
}
# 数据流向
终端=>信令服务
终端=>信令服务-[终端配置]>终端
终端=>信令服务-[终端上线])终端
```

### 关闭终端信令（client::shutdown）

```
# 消息主体
{}
# 数据流向
信令服务->终端
```

### 终端状态信令（client::status）

没有指定终端ID返回请求终端状态

```
# 消息主体
{
    "clientId": "终端ID（可选）"
}
{
    "ip"             : "终端IP",
    "name"           : "终端名称",
    "clientId"       : "终端ID",
    "clientType"     : "终端类型",
    "latitude"       : 纬度,
    "longitude"      : 经度,
    "humidity"       : 湿度,
    "temperature"    : 温度,
    "signal"         : 信号强度（0~100）,
    "battery"        : 电池电量（0~100）,
    "alarming"       : 是否发生告警（true|false）,
    "charging"       : 是否正在充电（true|false）,
    "clientRecording": 是否正在录像（true|false）,
    "serverRecording": 是否正在录像（true|false）,
    "lastHeartbeat"  : "最后心跳时间",
    "status"         : {更多状态},
    "config"         : {更多配置}
}
# 数据流向
终端=>信令服务
```

### 终端单播信令（client::unicast）

```
# 消息主体
{
    "to": "接收终端ID",
    ...
}
{
    ...
}
# 数据流向
终端->信令服务->终端
```

### 响铃信令（control::bell）

```
# 消息主体
{
    "to"     : "目标终端ID",
    "enabled": 是否响铃（true|false）
}
# 数据流向
信令服务=>终端
终端=>信令服务->终端
```

### 终端录像信令（control::client::record）

```
# 消息主体
{
    "to"     : "目标终端ID",
    "enabled": 是否录像（true|false）
}
{
    "enabled" : 是否录像（true|false）,
    "filepath": "视频文件路径"
}
# 数据流向
信令服务=>终端
终端=>信令服务->终端
```

### 配置音频信令（control::config::audio）

如果没有指定参数使用默认参数配置

```
# 消息主体
{
    "to": "目标终端ID",
    ...MediaAudioProperties
}
# 数据流向
信令服务=>终端
终端=>信令服务->终端
```

### 配置视频信令（control::config::video）

如果没有指定参数使用默认参数配置

```
# 消息主体
{
    "to": "目标终端ID",
    ...MediaVideoProperties
}
# 数据流向
信令服务=>终端
终端=>信令服务->终端
```

### 拍照信令（control::photograph）

```
# 消息主体
{
    "to": "目标终端ID"
}
{
    "filepath": "图片文件路径"
}
# 数据流向
信令服务=>终端
终端=>信令服务->终端
```

### 服务端录像信令（control::server::record）

```
# 消息主体
{
    "to"     : "目标终端ID",
    "roomId" : "房间ID",
    "enabled": 是否录像（true|false）
}
{
    "roomId"  : "房间ID",
    "enabled" : 是否录像（true|false）,
    "filepath": "视频文件路径",
    "clientId": "录像终端ID"
}
# 数据流向
终端=>信令服务->终端
```

### 终端唤醒信令（control::wakeup）

```
# 消息主体
{
    "to": "目标终端ID"
}
# 数据流向
信令服务=>终端
终端=>信令服务->终端
```

### 终端音量信令（media::audio::volume）

```
# 消息主体
{
    "roomId" : "房间ID",
    "volumes": [
        {
            "volume"  : 音量,
            "clientId": "终端ID"
        },
        ...
    ]
}
# 数据流向
媒体服务->信令服务-)终端
```

### 消费媒体信令（media::consume）

消费媒体：主动消费、终端生产媒体、终端创建WebRTC消费通道
终端生产媒体当前房间所有终端根据订阅类型自动消费媒体
终端创建WebRTC消费通道根据订阅类型自动消费当前房间已有媒体

```
# 消息主体
{
    "roomId"    : "房间ID",
    "producerId": "生产者ID"
}
{
    "roomId"        : "房间ID",
    "clientId"      : "消费者ID",
    "sourceId"      : "生产者ID",
    "streamId"      : "媒体ID",
    "producerId"    : "生产者ID",
    "consumerId"    : "消费者ID",
    "kind"          : "消费者媒体类型",
    "type"          : "消费者类型",
    "appData"       : "APP数据",
    "rtpParameters" : "RTP参数",
    "producerPaused": "生产者是否暂停",
}
# 数据流向
终端->信令服务->媒体服务=>信令服务->终端
终端-[生产媒体]>信令服务-[消费媒体])信令服务=>信令服务->终端
终端-[创建WebRTC通道]>信令服务-[消费媒体])信令服务=>信令服务->终端
```

### 关闭消费者信令（media::consumer::close）

关闭通过回调实现所以不能同步响应

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "消费者ID"
}
# 数据流向
媒体服务->信令服务->终端
信令服务->媒体服务->信令服务->终端
终端->信令服务->媒体服务->信令服务->终端
```

### 消费者空间层和时间层改变信令（media::consumer::layers::change）

```
# 消息主体
{
    "roomId"       : "房间ID"
    "consumerId"   : "消费者ID",
    "spatialLayer" : 最佳空间层,
    "temporalLayer": 最佳时间层
}
# 数据流向
媒体服务->信令服务+)终端
```

### 暂停消费者信令（media::consumer::pause）

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "消费者ID"
}
# 数据流向
终端->信令服务->媒体服务->信令服务->终端
终端-[暂停生产者]>信令服务->媒体服务-[暂停消费者]>信令服务->终端
```

### 请求关键帧信令（media::consumer::request::key::frame）

```
# 消息主体
{
    "roomId"    : "房间ID",
    "consumerId": "消费者ID"
}
# 数据流向
终端->信令服务->媒体服务
```

### 恢复消费者信令（media::consumer::resume）

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "消费者ID"
}
# 数据流向
终端->信令服务->媒体服务->信令服务->终端
终端-[恢复生产者]>信令服务->媒体服务-[恢复消费者]>信令服务->终端
```

### 媒体消费者评分信令（media::consumer::score）

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "消费者ID"
    "score"     : {
        "score"         : 消费者RTP流得分表示传输质量：0~10,
        "producerScore" : 生产者RTP流得分表示传输质量：0~10,
        "producerScores": [Simulcast生产者RTP流得分]
    }
}
# 数据流向
媒体服务->信令服务+)终端
```

### 修改最佳空间层和时间层信令（media::consumer::set::preferred::layers）

```
# 消息主体
{
    "roomId"       : "房间ID",
    "consumerId"   : "消费者ID",
    "spatialLayer" : 最佳空间层,
    "temporalLayer": 最佳时间层
}
# 数据流向
终端->信令服务->媒体服务
```

### 设置消费者优先级信令（media::consumer::set::priority）

如果优先级不在范围内表示取消优先级设置

```
# 消息主体
{
    "roomId"    : "房间ID",
    "consumerId": "消费者ID",
    "priority"  : 优先级（1~255）
}
# 数据流向
终端->信令服务->媒体服务
```

### 查询消费者状态信令（media::consumer::status）

```
# 消息主体
{
    "roomId"    : "房间ID",
    "consumerId": "消费者ID"
}
{
    "roomId"    : "房间ID",
    "consumerId": "消费者ID",
    "status"    : [
        ...状态信息
    ]
}
# 数据流向
终端=>信令服务->媒体服务
```

### 消费数据信令（media::data::consume）

数据通道消费者不会自动创建，需要用户自己订阅生产者。

```
# 消息主体
{
    "roomId"    : "房间ID"
    "producerId": "生产者ID",
}
# 数据流向
终端->信令服务->媒体服务->信令服务->终端
```

### 关闭数据消费者信令（media::data::consumer::close）

关闭通过回调实现所以不能同步响应，以下场景出现：主动断开、离开房间、信令断开

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "数据消费者ID"
}
# 数据流向
媒体服务->信令服务->终端
信令服务->媒体服务->信令服务->终端
终端->信令服务->媒体服务->信令服务->终端
```

### 查询数据消费者状态信令（media::data::consumer::status）

```
# 消息主体
{
    "roomId"    : "房间ID",
    "consumerId": "数据消费者ID"
}
{
    "roomId"    : "房间ID",
    "consumerId": "数据消费者ID",
    "status"    : [
        ...状态信息
    ]
}
# 数据流向
终端=>信令服务->媒体服务
```

### 生产数据信令（media::data::produce）

```
# 消息主体
{
    "roomId"     : "房间标识",
    "transportId": "通道标识"
}
{
    "roomId"    : "房间ID",
    "producerId": "生产者ID",
}
# 数据流向
终端=>信令服务->媒体服务
```

### 关闭数据生产者信令（media::data::producer::close）

关闭通过回调实现所以不能同步响应，以下场景出现：主动断开、离开房间、信令断开

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "数据生产者ID"
}
# 数据流向
媒体服务->信令服务->终端
信令服务->媒体服务->信令服务->终端
终端->信令服务->媒体服务->信令服务->终端
```

### 查询数据生产者状态信令（media::data::producer::status）

```
# 消息主体
{
    "roomId"    : "房间ID",
    "producerId": "数据生产者ID"
}
{
    "roomId"    : "房间ID",
    "producerId": "数据生产者ID",
    "status"    : [
        ...状态信息
    ]
}
# 数据流向
终端=>信令服务->媒体服务
```

### 重启ICE信令（media::ice::restart）

```
# 消息主体
{
    "roomId"     : "房间标识",
    "transportId": "通道标识"
}
{
    "roomId"       : "房间标识",
    "transportId"  : "通道标识",
    "iceParameters": "iceParameters"
}
# 数据流向
终端=>信令服务->媒体服务
```

### 生产媒体信令（media::produce）

```
# 消息主体
{
    "kind"         : "媒体类型",
    "roomId"       : "房间ID",
    "transportId"  : "通道ID",
    "rtpParameters": "rtpParameters"
}
{
    "kind"      : "媒体类型",
    "roomId"    : "房间ID",
    "producerId": "生产者ID",
}
# 数据流向
终端=>信令服务->媒体服务
```

### 关闭生产者信令（media::producer::close）

关闭通过回调实现所以不能同步响应，以下场景出现：主动断开、离开房间、信令断开

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "生产者ID"
}
# 数据流向
媒体服务->信令服务->终端
信令服务->媒体服务->信令服务->终端
终端->信令服务->媒体服务->信令服务->终端
```

### 暂停生产者信令（media::producer::pause）

```
# 消息主体
{
    "roomId"    : "房间ID"
    "producerId": "生产者ID"
}
# 数据流向
终端->信令服务->媒体服务->信令服务->终端
```

### 恢复生产者信令（media::producer::resume）

```
# 消息主体
{
    "roomId"    : "房间ID"
    "producerId": "生产者ID"
}
# 数据流向
终端->信令服务->媒体服务->信令服务->终端
```

### 媒体生产者评分信令（media::producer::score）

```
# 消息主体
{
    "roomId"    : "房间ID"
    "consumerId": "消费者ID"
    "score"     : {
        ...生产者评分
    }
}
# 数据流向
媒体服务->信令服务+)终端
```

### 查询生产者状态信令（media::producer::status）

```
# 消息主体
{
    "roomId"    : "房间ID",
    "producerId": "生产者ID"
}
{
    "roomId"    : "房间ID",
    "producerId": "生产者ID",
    "status"    : [
        ...状态信息
    ]
}
# 数据流向
终端=>信令服务->媒体服务
```

### 路由RTP协商信令（media::router::rtp::capabilities）

```
# 消息主体
{
    "roomId": "房间标识"
}
"roomId"         : "房间标识",
"rtpCapabilities": {
    "codec"           : "编码解码",
    "headerExtensions": "扩展"
}
# 数据流向
终端=>信令服务->媒体服务
```

### 关闭通道信令（media::transport::close）

关闭通过回调实现所以不能同步响应，以下场景出现：主动断开、离开房间、信令断开

```
# 消息主体
{
    "roomId"     : "房间ID"
    "transportId": "通道ID"
}
# 数据流向
媒体服务->信令服务->终端
信令服务->媒体服务->信令服务->终端
终端->信令服务->媒体服务->信令服务->终端
```

### 创建RTP输入通道信令（media::transport::plain::create）

用来接入RTP协议终端

```
# 消息主体
{
    "roomId"         : "房间ID",
    "rtcpMux"        : RTP/RTCP端口复用（true|false）,
    "comedia"        : 自动识别终端端口（true|false）,
    "enableSctp"     : 是否开启SCTP（true|false）,
    "numSctpStreams" : SCTP数量,
    "enableSrtp"     : 是否开启SRTP（true|false）,
    "srtpCryptoSuite": {
        "cryptoSuite": "算法（AEAD_AES_256_GCM|AEAD_AES_128_GCM|AES_CM_128_HMAC_SHA1_80|AES_CM_128_HMAC_SHA1_32）",
        "keyBase64"  : "密钥"
    }
}
{
    roomId     : "房间ID",
    transportId: "通道ID",
    ip         : "RTP监听IP",
    port       : "RTP媒体端口",
    rtcpPort   : "RTP媒体RTCP端口"
}
# 数据流向
终端=>信令服务->媒体服务
```

### 查询通道状态信令（media::transport::status）

```
# 消息主体
{
    "roomId"     : "房间ID",
    "transportId": "通道ID"
}
{
    "roomId"     : "房间ID",
    "transportId": "通道ID",
    "status"     : [
        ...状态信息
    ]
}
# 数据流向
终端=>信令服务->媒体服务
```

### 连接WebRTC通道信令（media::transport::webrtc::connect）

```
# 消息主体
{
    "roomId"        : "房间ID",
    "transportId"   : "通道ID",
    "dtlsParameters": "DTLS参数"
}
{
    "roomId"        : "房间ID",
    "transportId"   : "传输通道标识"
}
# 数据流向
终端=>信令服务->媒体服务
```

### 创建WebRTC通道信令（media::transport::webrtc::create）

```
# 消息主体
{
    "roomId"          : "房间ID",
    "forceTcp"        : "强制使用TCP",
    "producing"       : "是否生产",
    "consuming"       : "是否消费",
    "sctpCapabilities": "sctpCapabilities"
}
{
    "roomId"        : "房间ID",
    "transportId"   : "传输通道ID",
    "iceCandidates" : "iceCandidates",
    "iceParameters" : "iceParameters",
    "dtlsParameters": "dtlsParameters",
    "sctpParameters": "sctpParameters"
}
# 数据流向
终端=>信令服务->媒体服务
```

### 视频方向变化信令（media::video::orientation::change）

```
# 消息主体
{
    "roomId"    : "房间ID",
    "producerId": "生产者ID",
    "flip"      : "是否翻转",
    "camera"    : "是否摄像头",
    "rotation"  : "旋转角度"
}
# 数据流向
媒体服务->信令服务->终端
```

### 平台异常信令（platform::error）

```
# 消息主体
{}
# 数据流向
终端=>信令服务
信令服务->终端
```

### 重启平台信令（platform::reboot）

```
# 消息主体
{}
# 数据流向
信令服务+)终端
终端=>信令服务
终端->信令服务+)终端
```

### 执行命令信令（platform::script）

```
# 消息主体
{
    "script": "命令"
}
{
    "result": "结果"
}
# 数据流向
终端=>信令服务
```

### 关闭平台信令（platform::shutdown）

```
# 消息主体
{}
# 数据流向
信令服务+)终端
终端=>信令服务
终端->信令服务+)终端
```

### 房间广播信令（room::broadcast）

```
# 消息主体
{
    "roomId": "房间ID",
    ...
}
# 数据流向
终端->信令服务-)终端
```

### 房间终端列表信令（room::client::list）

```
# 消息主体
{
    "roomId": "房间ID"
}
[
    {
        "ip"             : "终端IP",
        "name"           : "终端名称",
        "clientId"       : "终端ID",
        "clientType"     : "终端类型",
        "latitude"       : 纬度,
        "longitude"      : 经度,
        "humidity"       : 湿度,
        "temperature"    : 温度,
        "signal"         : 信号强度（0~100）,
        "battery"        : 电池电量（0~100）,
        "alarming"       : 是否发生告警（true|false）,
        "charging"       : 是否正在充电（true|false）,
        "clientRecording": 是否正在录像（true|false）,
        "serverRecording": 是否正在录像（true|false）,
        "lastHeartbeat"  : "最后心跳时间",
        "status"         : {更多状态},
        "config"         : {更多配置}
    },
    ...
]
# 数据流向
终端=>信令服务
终端=[进入房间]>信令服务->终端
```

### 房间终端ID信令（room::client::list::id）

终端所有ID集合：消费者、生产者等等

```
# 消息主体
{
    "roomId"  : "房间ID",
    "clientId": "终端ID（可选）"
}
{
    "roomId"        : "房间ID",
    "clientId"      : "终端ID",
    "dataProducers" : "数据生产者ID集合",
    "dataConsumers" : "数据消费者ID集合",
    "audioProducers": "音频生产者ID集合",
    "videoProducers": "视频生产者ID集合",
    "audioConsumers": "音频消费者ID集合",
    "videoConsumers": "视频消费者ID集合"
}
# 数据流向
终端=>信令服务
```

### 关闭房间信令（room::close）

```
# 消息主体
{
    "roomId": "房间ID"
}
# 数据流向
终端->信令服务->媒体服务->信令服务+)终端
```

### 创建房间信令（room::create）

```
# 消息主体
{
    "name"         : "房间名称",
    "passowrd"     : "房间密码（选填）",
    "mediaClientId": "媒体服务ID"
}
{
    "roomId"       : "房间ID",
    "name"         : "房间名称",
    "clientSize"   : "终端数量",
    "mediaClientId": "媒体服务ID"
}
# 数据流向
终端=>信令服务->媒体服务
终端->信令服务->媒体服务->信令服务-)终端
```

### 进入房间信令（room::enter）

```
# 消息主体
{
    "roomId"  : "房间ID",
    "password": "房间密码（选填）"
}
{
    "roomId"  : "房间标识",
    "clientId": "终端标识"
}
# 数据流向
终端=>信令服务
终端->信令服务-)终端
```

### 踢出房间信令（room::expel）

终端收到信令以后调用离开房间信令离开房间，没有实现强制在服务端提出。

```
# 消息主体
{
    "roomId"  : "房间ID",
    "clientId": "终端ID"
}
# 数据流向
终端->信令服务->终端
```

### 邀请终端信令（room::invite）

终端收到信令以后调用进入房间信令进入房间

```
# 消息主体
{
    "roomId"  : "房间ID",
    "clientId": "终端ID",
    "password": "密码（选填）"
}
# 数据流向
终端->信令服务->终端
```

### 离开房间信令（room::leave）

```
# 消息主体
{
    "roomId": "房间ID"
}
{
    "roomId"  : "房间ID"
    "clientId": "离开终端ID"
}
# 数据流向
终端->信令服务-)终端
终端-[关闭终端]>信令服务-)终端
```

### 房间列表信令（room::list）

```
# 消息主体
{
    "roomId": "房间ID"
}
[
    {
        "name"         : "房间名称",
        "passowrd"     : "房间密码",
        "clientSize"   : "终端数量",
        "mediaClientId": "媒体服务标识"
    },
    ...
]
# 数据流向
终端=>信令服务
```

### 房间状态信令（room::status）

```
# 消息主体
{
    "roomId": "房间ID"
}
{
    "name"         : "房间名称",
    "passowrd"     : "房间密码",
    "clientSize"   : "终端数量",
    "mediaClientId": "媒体服务标识"
}
# 数据流向
终端=>信令服务
```

### 发起会话信令（session::call）

```
# 消息主体
{
    "clientId": "目标ID",
    "audio"   : 是否需要声音（true|false），
    "video"   : 是否需要视频（true|false）
}
{
    "name"     : "终端名称",
    "clientId" : "终端ID",
    "sessionId": "会话ID"
}
# 数据流向
终端=>信令服务->终端
```

### 关闭媒体信令（session::close）

```
# 消息主体
{
    "sessionId": "会话ID"
}
# 数据流向
终端->信令服务->终端
```

### 媒体交换信令（session::exchange）

媒体交换协商：offer/answer/candidate
    安卓需要注意：
        1. 交换类型大小写
        2. candidate内容默认名称sdp

```
# 消息主体
{
    "sdp"      : "sdp"
    "type"     : "offer|answer",
    "sessionId": "会话ID"
}
{
    "type"     : "candidate",
    "sessionId": "会话ID",
    "candidate": {
        "sdpMid"       : "sdpMid",
        "candidate"    : "candidate",
        "sdpMLineIndex": sdpMLineIndex
    }
}
# 数据流向
终端->信令服务->终端
```

### 暂停媒体信令（session::pause）

```
# 消息主体
{
    "sessionId": "会话ID",
    "type"     : "媒体类型（audio|voice）"
}
# 数据流向
终端->信令服务->终端
```

### 恢复媒体信令（session::resume）

```
# 消息主体
{
    "sessionId": "会话ID",
    "type"     : "媒体类型（audio|voice）"
}
# 数据流向
终端->信令服务->终端
```

### 系统信息信令（system::info）

```
# 消息主体
{
    "diskspace": [
        {
            "path" : "存储路径",
            "free" : 存储空闲,
            "total": 存储总量
        },
        ...
    ],
    "maxMemory"    : 最大能用内存,
    "freeMemory"   : 空闲内存,
    "totalMemory"  : 已用内存,
    "osArch"       : "系统架构",
    "osName"       : "系统名称",
    "osVersion"    : "系统版本",
    "javaVmName"   : "虚拟机名称",
    "javaVersion"  : "虚拟机版本",
    "cpuProcessors": CPU核心数量
}
# 数据流向
终端=>信令服务
```

### 重启系统信令（system::reboot）

重启系统

```
# 消息主体
{}
# 数据流向
信令服务+)终端
终端=>信令服务+)终端
```

### 关闭系统信令（system::shutdown）

关闭系统

```
# 消息主体
{}
# 数据流向
信令服务+)终端
终端=>信令服务+)终端
```

