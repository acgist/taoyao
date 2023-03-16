# 部署

## 整体环境

```
CentOS：CentOS Linux release 7.9.2009 (Core)
git >= 1.8.0
pm2 >= 5.2.0
Java >= 17.0.0
Maven >= 3.8.0
gcc/g++ >= 10.2.0
node version >= v16.19.0
python version >= 3.8.0 with PIP
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
vim ~/.bash_profile

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
mkdir -p /data/dev/nodejs
cd /data/dev/nodejs
wget https://nodejs.org/dist/v16.19.0/node-v16.19.0-linux-x64.tar.xz
xz -d node-v16.19.0-linux-x64.tar.xz
tar -xf node-v16.19.0-linux-x64.tar

# 连接
ln -sf /data/dev/nodejs/node-v16.19.0-linux-x64/bin/npm /usr/local/bin/
ln -sf /data/dev/nodejs/node-v16.19.0-linux-x64/bin/node /usr/local/bin/

# 验证
npm -v
node -v
```

## 安装PM2

```
# 安装
npm install -g pm2

# 连接
ln -sf /data/dev/nodejs/node-v16.19.0-linux-x64/bin/pm2 /usr/local/bin/

# 日志
pm2 install pm2-logrotate
pm2 set pm2-logrotate:retain 14
pm2 set pm2-logrotate:compress true
pm2 set pm2-logrotate:max_size 256M

# 查看配置
pm2 conf

# 自启
pm2 startup
pm2 save
```

## 安装Java

```
# 下载
mkdir -p /data/dev/java
cd /data/dev/java
wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz
tar -zxvf openjdk-17.0.2_linux-x64_bin.tar.gz

# 配置
vim ~/.bash_profile

---
JAVA_HOME=/data/dev/java/jdk-17.0.2
PATH=$PATH:$JAVA_HOME/bin
---

# 立即生效
. ~/.bash_profile

# 验证
java -version
```

## 安装Maven

```
# 下载
mkdir -p /data/dev/maven
cd /data/dev/maven
wget https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz
tar -zxvf apache-maven-3.8.8-bin.tar.gz

# 配置
vim ~/.bash_profile

---
MAVEN_HOME=/data/dev/maven/apache-maven-3.8.8
PATH=$PATH:$MAVEN_HOME/bin
---

# 立即生效
. ~/.bash_profile

# 验证
mvn -version
```

## 安装Python

```
# 依赖
yum install zlib-devel libffi-devel openssl-devel

# 下载
mkdir -p /data/dev/python
cd /data/dev/python
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

# 设置镜像
mkdir -p ~/.pip/
vim ~/.pip/pip.conf

---
[global]
index-url = http://mirrors.aliyun.com/pypi/simple/
[install]
trusted-host = mirrors.aliyun.com
---

# 验证镜像
pip config list
```

## 安装Nginx

```
# 安装
rpm -Uvh http://nginx.org/packages/centos/7/noarch/RPMS/nginx-release-centos-7-0.el7.ngx.noarch.rpm
yum install nginx

# 配置服务
systemctl enable nginx

# 管理服务
systemctl start | stop | restart nginx

# 加载配置
nginx -s reload

# 权限问题
vim /etc/selinux/config

---
SELINUX=disabled
---
```

## 下载源码

```
cd /data
git clone https://gitee.com/acgist/taoyao.git --recursive
```

## 安装媒体

```
# 编译代码
cd /data/taoyao/taoyao-client-media
npm install

# 配置ecosystem
pm2 start | reload ecosystem.config.json
pm2 save

# 管理服务：服务名称必须和配置终端标识一致否则不能执行重启和关闭信令
pm2 start | stop | restart taoyao-client-media
```

### Mediasoup编译失败

编译过程中的依赖下载容易失败，
需要进入目录`mediasoup/worker/subprojects`，查看`*.wrap`文件依次下载所需依赖，修改名称放到`packagefiles`目录中，最后注释下载链接。
将`package.json`中的`mediasoup`改为本地依赖`file:./mediasoup`，重新编译即可。

> 下载依赖建议备份以备以后编译使用

### Mediasoup单独编译

编译媒体服务时会自动编译`mediasoup`所以可以不用单独编译

```
# 编译代码
# make -C worker
cd /data/taoyao/taoyao-client-media/mediasoup/worker
make

# 清理结果
make clean
```

## 安装信令

```
# 编译代码
cd /data/taoyao/taoyao-signal-server
mvn clean package -D skipTests
#mvn clean package -D skipTests -P prd

# 拷贝脚本
cp taoyao-server/target/taoyao-server-1.0.0/bin/deploy.sh ./

# 配置服务
cp /data/taoyao/docs/etc/taoyao-signal-server.service /usr/lib/systemd/system/taoyao-signal-server.service

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
cd /data/taoyao/taoyao-client-web
npm install

# 配置服务
pm2 start npm --name "taoyao-client-web" -- run dev
pm2 save

# 管理服务
pm2 start | stop | restart taoyao-client-web

# 打包代码
npm run build

# Nginx配置
cp /data/taoyao/docs/etc/nginx.conf /etc/nginx/nginx.conf

nginx -s reload
```

## 配置防火墙

```
# 终端服务：建议使用Nginx代理
firewall-cmd --zone=public --add-port=8443/tcp --permanent
# 信令服务（WebSocket）
firewall-cmd --zone=public --add-port=8888/tcp --permanent
# 信令服务（Socket）：没有启用不用添加规则
firewall-cmd --zone=public --add-port=9999/tcp --permanent
# 媒体服务（数据）
firewall-cmd --zone=public --add-port=40000-49999/udp --permanent

firewall-cmd --reload
firewall-cmd --list-ports

# 删除端口
#firewall-cmd --zone=public --remove-port=8443/tcp --permanent
#firewall-cmd --zone=public --remove-port=8888/tcp --permanent
#firewall-cmd --zone=public --remove-port=9999/tcp --permanent
#firewall-cmd --zone=public --remove-port=40000-49999/udp --permanent
```

## 证书

```
mkdir /data/certs
cd /data/certs
vim server.ext

---
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth
subjectAltName=@SubjectAlternativeName

[ SubjectAlternativeName ]
IP.1=127.0.0.1
IP.2=192.168.1.100
IP.3=192.168.1.110
IP.4=192.168.8.100
IP.5=192.168.8.110
DNS.1=localhost
DNS.2=acgist.com
DNS.3=www.acgist.com
DNS.4=taoyao.acgist.com
---

# CA
openssl genrsa -out ca.key 2048
openssl req -x509 -new -key ca.key -out ca.crt -days 3650
openssl x509 -in ca.crt -subject -issuer -noout
# subject= /C=cn/ST=gd/L=gz/O=acgist/OU=acgist/CN=acgist.com
# issuer= /C=cn/ST=gd/L=gz/O=acgist/OU=acgist/CN=acgist.com

# Server

openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr
openssl x509 -req -in server.csr -out server.crt -CA ca.crt -CAkey ca.key -CAcreateserial -days 3650 -extfile server.ext
openssl x509 -in server.crt -subject -issuer -noout
# subject= /C=cn/ST=gd/L=gz/O=acgist/OU=taoyao/CN=taoyao.acgist.com
# issuer= /C=cn/ST=gd/L=gz/O=acgist/OU=acgist/CN=acgist.com
openssl pkcs12 -export -clcerts -in server.crt -inkey server.key -out server.p12 -name taoyao
```
