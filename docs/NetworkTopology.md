# 拓扑网络

多子网环境下的部署方案，当前设计只在顶层子网部署服务，每层子网采用双网卡服务器通过防火墙自动转发实现。

## 两个子网

```
# 配置`TURN`服务
`coturn`

#配置地址重写
`ip-rewrite`
```

## 三个及其以上子网

### 配置端口转发

```
# 配置转发：两个配置一个即可
vim /etc/default/ufw
---
DEFAULT_FORWARD_POLICY="ACCEPT"
---
vim /etc/sysctl.conf  | vim /etc/ufw/sysctl.conf
---
net.ipv4.ip_forward=1 | net/ipv4/ip_forward=1
---
sysctl -p

# *filter之前添加
vim /etc/ufw/before.rules
---
*nat
:PREROUTING ACCEPT [0:0]
:POSTROUTING ACCEPT [0:0]
# TODO：补充完整规则
-A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
-A POSTROUTING -j MASQUERADE
COMMIT
---
ufw reload

# 配置`TURN`服务
`coturn`

#配置地址重写
`ip-rewrite`
```

## 端口转发规则

* DNAT：      目标IP转换
* SNAT：      源IP转换
* REDIRECT：  端口重定向
* MASQUERADE：源地址动态伪装

```
-A PREROUTING                    -p tcp --dport 80 -j REDIRECT   --to-port        8080
-A PREROUTING  -d 192.168.1.100  -p tcp --dport 80 -j REDIRECT   --to-port        8080
-A PREROUTING                    -p tcp --dport 80 -j DNAT       --to-destination 192.168.2.100:8080
-A PREROUTING  -d 192.168.1.100  -p tcp --dport 80 -j DNAT       --to-destination 192.168.2.100:8080
-A POSTROUTING                   -p tcp --dport 80 -j SNAT       --to-source      192.168.2.100
-A POSTROUTING -s 192.168.1.0/24                   -j SNAT       --to-source      192.168.2.100
-A POSTROUTING                                     -j MASQUERADE
-A POSTROUTING -s 192.168.1.0/24                   -j MASQUERADE
```

## iptables

### 清除端口转发规则

```
# 查看nat
iptables -L -t nat
# 清理nat
iptables -F -t nat
```

### 四张表

* nat：   NAT功能（端口映射、地址映射等等）
* raw：   优先级最高
* filter：过滤功能
* mangle：修改特定数据包

### 五条链

* INPUT：      通过路由表后目的地为本机
* OUTPUT：     由本机产生向外转发
* FORWARDING： 通过路由表后目的地不为本机
* PREROUTING： 数据包进入路由表之前
* POSTROUTING：发送到网卡接口之前
