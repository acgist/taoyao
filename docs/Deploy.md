# 部署

## Yum源

```
cd /etc/yum.repos.d
rm -rf *
wget /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo
yum makecache
```

## Linux句柄数量

```
vim /etc/security/limits.conf

root soft nofile 655350
root hard nofile 655350
* soft nofile 655350
* hard nofile 655350
* soft nproc 655350
* hard nproc 655350
* soft core unlimited
* hard core unlimited
```

## Linux内核优化

```
vim /etc/sysctl.conf

net.ipv4.tcp_tw_reuse        = 1
net.ipv4.tcp_tw_recycle      = 1
net.ipv4.tcp_syncookies      = 1
net.ipv4.tcp_fin_timeout     = 30
net.ipv4.tcp_max_tw_buckets  = 8192
net.ipv4.tcp_max_syn_backlog = 8192

# 其他
net.core.rmem_max
net.core.rmem_default
net.core.wmem_max
net.core.wmem_default
net.core.somaxconn           = 1024
net.core.netdev_max_backlog  = 8092
net.ipv4.udp_mem
net.ipv4.udp_rmem
net.ipv4.udp_wmem
net.ipv4.tcp_mem             = 78643200 104857600 157286400
net.ipv4.tcp_rmem            = 873200 1746400 3492800
net.ipv4.tcp_wmem            = 873200 1746400 3492800

sysctl -p
```

## Git

```
yum install git
```

# KMS

```
```

## Java

安装之前需要卸载旧版，如果旧版已经是`17+`可以忽略安装。

```
# 下载
mkdir -p /data/java
cd /data/java
wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz
tar -zxvf openjdk-17.0.2_linux-x64_bin.tar.gz
# 环境变量
vim ~/.bash_profile
PATH=$PATH:/data/java/jdk-17.0.2/bin
. ~/.bash_profile
ln -sf /data/java/jdk-17.0.2/bin/java /usr/local/bin/java
# 验证
java -version
```

## Maven

```
# 下载
mkdir -p /data/maven
cd /data/maven
wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz
tar -zxvf apache-maven-3.8.6-bin.tar.gz
# 环境变量
vim ~/.bash_profile
PATH=$PATH:/data/maven/apache-maven-3.8.6/bin
. ~/.bash_profile
# 验证
mvn -version
```

## Taoyao

```
# 下载源码
mkdir -p /data/taoyao
cd /data
git clone https://gitee.com/acgist/taoyao.git
cd /data/taoyao/taoyao

# 编译代码
mvn clean package -D skipTests
#mvn clean package -D skipTests -P release

# 拷贝脚本
cp taoyao-server/target/taoyao-server-1.0.0/bin/deploy.sh ../

# 启动服务
vim /usr/lib/systemd/system/taoyao.service
----
[Unit]
Description=桃夭
After=network.target
Wants=network.target

[Service]
User=root
Type=forking
KillMode=process
ExecStart=/data/taoyao/taoyao-server/bin/startup.sh
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

## 防火墙

```
firewall-cmd --zone=public --add-port=8888/tcp --permanent
firewall-cmd --zone=public --add-port=45535-65535/udp --permanent

firewall-cmd --reload
firewall-cmd --list-ports

# 删除端口
firewall-cmd --zone=public --remove-port=8888/udp --permanent
firewall-cmd --zone=public --remove-port=45535-65535/udp --permanent
```

## 证书

正式环境建议关闭项目`SSL`配置，可以使用`Nginx`配置证书。

```
keytool -genkeypair -keyalg RSA -dname "CN=localhost, OU=acgist, O=taoyao, L=GZ, ST=GD, C=CN" -alias taoyao -validity 3650 -ext ku:c=dig,keyE -ext eku=serverAuth -ext SAN=dns:localhost,ip:127.0.0.1 -keystore taoyao.jks -keypass 123456 -storepass 123456
```

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