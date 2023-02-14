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

## 安装PM2

```
# 安装
npm install -g pm2

# 连接
ln -sf /data/nodejs/node-v16.19.0-linux-x64/bin/pm2 /usr/local/bin/

# 日志
pm2 install pm2-logrotate
pm2 set pm2-logrotate-ext:retain 14
pm2 set pm2-logrotate-ext:max_size 256M

# 自启
pm2 startup
pm2 save
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

## 安装Python

```
# 依赖
yum install zlib-devel
yum install libffi-devel
yum install openssl-devel

# 下载
mkdir -p /data/python
cd /data/python
#wget https://www.python.org/ftp/python/3.8.16/Python-3.8.16.tar.xz
wget https://mirrors.huaweicloud.com/python/3.8.16/Python-3.8.16.tar.xz
xz -d Python-3.8.16.tar.xz
tar -xf Python-3.8.16.tar

# 安装
cd Python-3.8.16
./configure --prefix=/usr/local/python3 --with-ssl
make && make install

# 配置
ln -sf /usr/local/python3/bin/pip3.8 /usr/bin/pip
ln -sf /usr/local/python3/bin/python3.8 /usr/bin/python
ln -sf /usr/local/python3/bin/python3.8 /usr/bin/python3

# 配置YUM

vim /usr/bin/yum
vim /usr/libexec/urlgrabber-ext-down

---
/usr/bin/python => /usr/bin/python2.7
---

## 验证
yum --version
pip --version
python --version
```

## 下载源码

```
cd /data
git clone https://gitee.com/acgist/taoyao.git --recursive
```

## 安装媒体

```
# 设置镜像
vim ~/.pip/pip.conf

---
[global]
index-url = http://mirrors.aliyun.com/pypi/simple/
[install]
trusted-host = mirrors.aliyun.com
---

# 验证镜像
pip config list

# 编译代码
cd /data/taoyao/taoyao-media-server
git submodule update --remote
cd modulesup
git checkout taoyao
cd ..
npm install

# 配置服务
pm2 start npm --name "taoyao-media-server" -- run dev | release
pm2 save

# 管理服务
pm2 start | stop | restart taoyao-media-server
```

### Mediasoup单独编译

编译媒体服务时会自动编译`mediasoup`所以可以不用单独编译

```
# 编译代码
# make -C worker
cd /data/taoyao/taoyao-media-server/mediasoup/worker
make

# 清理结果
make clean
```

### 问题

#### Subproject exists but has no meson.build file

编译过程需要第三方的依赖，进入目录`mediasoup/worker/subprojects`，查看`*.wrap`依次下载然后修改名称放到`packagecache`，重新编译即可。

## 安装信令

```
# 编译代码
cd /data/taoyao/taoyao-signal-server
mvn clean package -D skipTests
#mvn clean package -D skipTests -P release

# 拷贝脚本
cp taoyao-server/target/taoyao-server-1.0.0/bin/deploy.sh ./

# 配置服务
vim /usr/lib/systemd/system/taoyao-signal-server.service

----
[Unit]
Description=taoyao signal server
After=network.target
Wants=network.target

[Service]
User=root
Type=forking
KillMode=process
ExecStart=/data/taoyao/taoyao-signal-server/deploy/bin/startup.sh
ExecReload=/bin/kill -HUP $MAINPID
ExecStop=/bin/kill -QUIT $MAINPID
Restart=always
RestartSec=5s

[Install]
WantedBy=multi-user.target
----

# 配置自启
systemctl daemon-reload
systemctl enable taoyao-signal-server

# 执行脚本
./deploy.sh

# 管理服务
systemctl start | stop | restart taoyao-signal-server
```

## 安装终端

```
# 编译代码
cd /data/taoyao/taoyao-client
npm install

# 配置服务
pm2 start npm --name "taoyao-client" -- run dev
pm2 save

# 管理服务
pm2 start | stop | restart taoyao-client
```

## 配置防火墙

```
# 终端服务：建议使用Nginx代理
firewall-cmd --zone=public --add-port=8443/tcp --permanent
# 信令服务（Socket）：没有启用不用添加规则
firewall-cmd --zone=public --add-port=9999/tcp --permanent
# 信令服务（WebSocket）
firewall-cmd --zone=public --add-port=8888/tcp --permanent
# 媒体服务（控制）：只暴露给信令服务
firewall-cmd --zone=public --add-rich-rule="rule family="ipv4" source address="192.168.1.0/24" port protocol="tcp" port="4443" accept" --permanent
# 媒体服务（数据）
firewall-cmd --zone=public --add-port=40000-49999/udp --permanent

firewall-cmd --reload
firewall-cmd --list-ports

# 删除端口
#firewall-cmd --zone=public --remove-port=8443/tcp --permanent
#firewall-cmd --zone=public --remove-rich-rule="rule family="ipv4" source address="192.168.1.0/24" port protocol="tcp" port="4443" accept" --permanent
#firewall-cmd --zone=public --remove-port=9999/tcp --permanent
#firewall-cmd --zone=public --remove-port=8888/tcp --permanent
#firewall-cmd --zone=public --remove-port=40000-49999/udp --permanent
```

## 证书

正式环境建议关闭项目`SSL`配置，可以使用`Nginx`配置证书。

```
keytool -genkeypair -keyalg RSA -dname "CN=localhost, OU=acgist, O=taoyao, L=GZ, ST=GD, C=CN" -alias taoyao -validity 3650 -ext ku:c=dig,keyE -ext eku=serverAuth -ext SAN=dns:localhost,ip:127.0.0.1 -keystore taoyao.jks -keypass 123456 -storepass 123456
```
