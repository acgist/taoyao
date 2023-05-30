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

## STUN/TURN

视频房间不用`STUN/TURN`服务，视频会话需要自己搭建`coturn`服务。

## SDP格式

```
v=  (protocol version)                                                #协议版本
o=  (owner/creator and session identifier)                            #所有者创建者和会话标识符
s=  (session name)                                                    #会话名称
i=* (session information)                                             #会话信息
u=* (URI of description)                                              #URI描述
e=* (email address)                                                   #Email地址
p=* (phone number)                                                    #电话号码
c=* (connection information - not required if included in all media)  #连接信息（如果包含在所有媒体中则不需要该字段）
b=* (zero or more bandwidth information lines)                        #带宽信息
z=* (time zone adjustments)                                           #时区调整
k=* (encryption key)                                                  #加密密钥
a=* (zero or more session attribute lines)                            #0或多次会话属性
Time description                                                      #时间描述
t=  (time the session is active)                                      #会话活动时间
r=* (zero or more repeat times)                                       #0或多次重复次数
​Media description                                                     #媒体描述
m=  (media name and transport address)                                #媒体名称和传输地址
i=* (media title)                                                     #媒体标题
c=* (connection information - optional if included at session-level)  #连接信息（如果包含在会话层则该字段可选）
b=* (zero or more bandwidth information lines)                        #带宽信息
k=* (encryption key)                                                  #加密密钥
a=* (zero or more media attribute lines)                              #0或多个会话属性
````
