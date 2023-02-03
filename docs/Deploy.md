# 部署

## 整体环境

```
CentOS：CentOS Linux release 7.9.2009 (Core)
Java >= 17
Maven >= 3.6.0
gcc/g++ >= 4.9
node version >= v16.0.0
python version >= 3.6 with PIP
```

## 设置Yum源

```
cd /etc/yum.repos.d
rm -rf *
wget /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo
yum makecache
```

## 优化Linux句柄数量

```
# 配置
vim /etc/security/limits.conf

---
root soft nofile 655350
root hard nofile 655350
* soft nofile 655350
* hard nofile 655350
* soft nproc 655350
* hard nproc 655350
* soft core unlimited
* hard core unlimited
---

# 验证（重新打开窗口有效）
ulimit -a
```

## 优化Linux内核参数

```
# 配置
vim /etc/sysctl.conf

---
net.ipv4.tcp_tw_reuse        = 1
net.ipv4.tcp_tw_recycle      = 1
net.ipv4.tcp_syncookies      = 1
net.ipv4.tcp_fin_timeout     = 30
net.ipv4.tcp_keepalive_time  = 1200
net.ipv4.tcp_max_tw_buckets  = 8192
net.ipv4.tcp_max_syn_backlog = 8192
---

# 立即生效
sysctl -p
```

## 安装Git

```
# 安装
yum install git

# 验证
git --version
```

## 安装GCC/G++

```
# 安装
yum install centos-release-scl
yum install devtoolset-10-gcc devtoolset-10-gcc-c++
scl enable devtoolset-10 -- bash

# 配置
vim /etc/profile

---
source /opt/rh/devtoolset-10/enable
---

# 验证
gcc -v
g++ -v
```

## 安装Node

```
# 下载
mkdir -p /data/nodejs
cd /data/nodejs
wget https://nodejs.org/dist/v16.19.0/node-v16.19.0-linux-x64.tar.xz
xz -d node-v16.19.0-linux-x64.tar.xz
tar -xf node-v16.19.0-linux-x64.tar

# 连接
ln -sf /data/nodejs/node-v16.19.0-linux-x64/bin/npm /usr/local/bin/
ln -sf /data/nodejs/node-v16.19.0-linux-x64/bin/node /usr/local/bin/

# 验证
npm -v
node -v
```

## 安装Java

```
# 下载
mkdir -p /data/java
cd /data/java
wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz
tar -zxvf openjdk-17.0.2_linux-x64_bin.tar.gz

# 配置
vim ~/.bash_profile
PATH=$PATH:/data/java/jdk-17.0.2/bin
. ~/.bash_profile
ln -sf /data/java/jdk-17.0.2/bin/java /usr/local/bin/java

# 验证
java -version
```

## 安装Maven

```
# 下载
mkdir -p /data/maven
cd /data/maven
wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz
tar -zxvf apache-maven-3.8.6-bin.tar.gz

# 配置
vim ~/.bash_profile
PATH=$PATH:/data/maven/apache-maven-3.8.6/bin
. ~/.bash_profile

# 验证
mvn -version
```

## 下载源码

```
cd /data
git clone https://gitee.com/acgist/taoyao.git --recursive
```

## 安装媒体

```
# 编译代码
cd /data/taoyao/taoyao-media-server
npm install

# 启动媒体
```

## 安装信令

```
# 编译代码
cd /data/taoyao/taoyao-signal-server
mvn clean package -D skipTests
#mvn clean package -D skipTests -P release

# 拷贝脚本
cp taoyao-server/target/taoyao-server-1.0.0/bin/deploy.sh ./

# 配置启动服务
vim /usr/lib/systemd/system/taoyao-signal.service

----
[Unit]
Description=taoyao signal server
After=network.target
Wants=network.target

[Service]
User=root
Type=forking
KillMode=process
ExecStart=/data/deploy/taoyao-signal-server/bin/startup.sh
ExecReload=/bin/kill -HUP $MAINPID
ExecStop=/bin/kill -QUIT $MAINPID
Restart=always
RestartSec=5s

[Install]
WantedBy=multi-user.target
----

systemctl daemon-reload
systemctl start taoyao
systemctl enable taoyao
```

## 安装终端

## 配置防火墙

### taoyao-media-server


```
firewall-cmd --zone=public --add-port=8888/tcp --permanent
# 媒体服务（数据）：40000-49999
firewall-cmd --zone=public --add-port=40000-49999/udp --permanent

firewall-cmd --reload
firewall-cmd --list-ports

# 删除端口
#firewall-cmd --zone=public --remove-port=8888/udp --permanent
#firewall-cmd --zone=public --remove-port=40000-49999/udp --permanent
```

## 证书

正式环境建议关闭项目`SSL`配置，可以使用`Nginx`配置证书。

```
keytool -genkeypair -keyalg RSA -dname "CN=localhost, OU=acgist, O=taoyao, L=GZ, ST=GD, C=CN" -alias taoyao -validity 3650 -ext ku:c=dig,keyE -ext eku=serverAuth -ext SAN=dns:localhost,ip:127.0.0.1 -keystore taoyao.jks -keypass 123456 -storepass 123456
```

## 资料

https://www.jianshu.com/p/fa047d7054eb
https://www.jianshu.com/p/59da3d350488
https://www.jianshu.com/p/fa047d7054eb
http://koca.szkingdom.com/forum/t/topic/218
https://segmentfault.com/a/1190000039782685
https://www.cnblogs.com/bolingcavalry/p/15473808.html
http://www.manoner.com/post/音视频基础/WebRTC核心组件和协议栈/
https://blog.csdn.net/eguid_1/article/details/117277841
https://blog.csdn.net/xiang_6119/article/details/108779678
https://blog.csdn.net/qq_40321119/article/details/108336324
https://blog.csdn.net/ababab12345/article/details/115585378
https://blog.csdn.net/m0_64867003/article/details/121901782
https://blog.csdn.net/jisuanji111111/article/details/121634199
https://blog.csdn.net/weixin_48638578/article/details/120191152
https://blog.csdn.net/weixin_45565568/article/details/108929438
https://blog.csdn.net/weixin_40425640/article/details/125444018
http://t.zoukankan.com/yjmyzz-p-webrtc-groupcall-using-kurento.html
https://lequ7.com/guan-yu-webrtc-yi-wen-xiang-jie-webrtc-ji-chu.html
