# 项目部署

## 整体环境

```
Debian  =  11.7.0
Git     >= 1.8.0
Java    >= 17.0.0
Maven   >= 3.8.0
CMake   >= 3.26.0
NodeJS  >= v18.16.0
Python  >= 3.8.0 with PIP
ffmpeg  >= 4.3.0
gcc/g++ >= 10.2.0
Android >= 9.0
```

## Debian

`CentOS 7`实在是太旧了，软件更新非常麻烦，所以直接使用`Debian`作为测试。

### 系统参数

* CPU = 1核
* 内存 = 1G
* 硬盘 = 20G
* 帐号 = taoyao

### 硬盘分区

* /     = 8G
* swap  = 2G
* /data = 16G

### 选择并安装软件很慢

```
Ctrl + Alt + F2
vi /target/etc/apt/sources.list

---
#deb http://security.debian.org/debian-security bullseye-security main
#deb-src http://security.debian.org/debian-security bullseye-security main
deb http://mirrors.ustc.edu.cn/debian-security bullseye-security main
deb-src http://mirrors.ustc.edu.cn/debian-security bullseye-security main
---

Ctrl + Alt + F1
```

### 配置vi

```
vi /etc/vim/vimrc.tiny

---
set backspace=2
set nocompatible
---
```

### 网络配置

```
# 配置
vi /etc/network/interfaces

---
auto enp0s3
iface enp0s3 inet static
address 192.168.1.110
gateway 192.168.1.1
netmask 255.255.255.0
---

# 重启网卡
ifdown enp0s3
ifup enp0s3
```

### 设置国内镜像

```
# DNS
sudo vim /etc/systemd/resolved.conf

---
DNS=233.5.5.5 233.6.6.6 114.114.114.114 8.8.8.8
---

sudo systemctl restart systemd-resolved
sudo systemctl enable systemd-resolved
sudo ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf

# 配置
vi /etc/apt/sources.list

---
deb https://mirrors.aliyun.com/debian/ bullseye main non-free contrib
deb-src https://mirrors.aliyun.com/debian/ bullseye main non-free contrib
deb https://mirrors.aliyun.com/debian-security/ bullseye-security main
deb-src https://mirrors.aliyun.com/debian-security/ bullseye-security main
deb https://mirrors.aliyun.com/debian/ bullseye-updates main non-free contrib
deb-src https://mirrors.aliyun.com/debian/ bullseye-updates main non-free contrib
deb https://mirrors.aliyun.com/debian/ bullseye-backports main non-free contrib
deb-src https://mirrors.aliyun.com/debian/ bullseye-backports main non-free contrib
---

# 更新系统
apt update
apt upgrade
```

### 安装依赖

```
# 常用工具
apt install vim sudo wget net-tools

# 配置sudo
chmod u+w /etc/sudoers
vim /etc/sudoers

---
taoyao  ALL=(ALL:ALL) ALL
---

chmod u-w /etc/sudoers
```

### 优化Linux句柄数量

```
# 配置
vim /etc/security/limits.conf

---
root soft nofile 655350
root hard nofile 655350
*    soft nofile 655350
*    hard nofile 655350
*    soft nproc  655350
*    hard nproc  655350
*    soft core   unlimited
*    hard core   unlimited
---

# 验证（重新打开窗口有效）
ulimit -a
```

### 优化Linux内核参数

```
# 配置
vim /etc/sysctl.conf

---
net.ipv4.tcp_tw_reuse        = 1
net.ipv4.tcp_syncookies      = 1
net.ipv4.tcp_fin_timeout     = 30
net.ipv4.tcp_keepalive_time  = 1200
net.ipv4.tcp_max_tw_buckets  = 8192
net.ipv4.tcp_max_syn_backlog = 8192
---

# 立即生效
sysctl -p
```

### 配置目录权限

```
chown taoyao /data
```

## 安装软件依赖

```
sudo apt install pkg-config libssl-dev zlib1g-dev
```

## 安装Git

```
# 安装
sudo apt install git

# 验证
git --version
```

## 安装gcc/g++

```
# 安装
sudo apt install build-essential

# 验证
gcc -v
g++ -v
```

## 安装Java

```
# 下载
mkdir -p /data/dev/java ; cd $_
wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz
tar -zxvf openjdk-17.0.2_linux-x64_bin.tar.gz

# 配置
vim ~/.profile

---
JAVA_HOME=/data/dev/java/jdk-17.0.2
PATH=$PATH:$JAVA_HOME/bin
---

# 立即生效
. ~/.profile

# 连接
sudo ln -sf /data/dev/java/jdk-17.0.2/bin/java /usr/local/bin/

# 验证
java -version
```

## 安装Maven

```
# 下载
mkdir -p /data/dev/maven ; cd $_
#wget https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz
wget https://mirrors.ustc.edu.cn/apache/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz
tar -zxvf apache-maven-3.8.8-bin.tar.gz

# 配置
vim ~/.profile

---
MAVEN_HOME=/data/dev/maven/apache-maven-3.8.8
PATH=$PATH:$MAVEN_HOME/bin
---

# 立即生效
. ~/.profile

# 连接
sudo ln -sf /data/dev/maven/apache-maven-3.8.8/bin/mvn /usr/local/bin/

# 验证
mvn -version
```

## 安装CMake

```
# 下载
mkdir -p /data/dev/cmake ; cd $_
wget https://github.com/Kitware/CMake/releases/download/v3.26.0/cmake-3.26.0.tar.gz

# 安装
tar -zxvf cmake-3.26.0.tar.gz
cd cmake-3.26.0
./configure
make && sudo make install

# 验证
cmake -version
```

## 安装NodeJS

```
# 下载
mkdir -p /data/dev/nodejs ; cd $_
wget https://nodejs.org/dist/v18.16.0/node-v18.16.0-linux-x64.tar.xz
tar -Jxvf node-v18.16.0-linux-x64.tar.xz

# 连接
sudo ln -sf /data/dev/nodejs/node-v18.16.0-linux-x64/bin/npm  /usr/local/bin/
sudo ln -sf /data/dev/nodejs/node-v18.16.0-linux-x64/bin/node /usr/local/bin/

# 设置镜像
npm config set registry https://registry.npm.taobao.org

# 安装pm2
npm install -g pm2

# 连接
sudo ln -sf /data/dev/nodejs/node-v18.16.0-linux-x64/bin/pm2 /usr/local/bin/

# 安装日志
pm2 install pm2-logrotate
pm2 set pm2-logrotate:retain 14
pm2 set pm2-logrotate:compress true
pm2 set pm2-logrotate:max_size 256M

# 自启
pm2 startup
#sudo env PATH=$PATH:/data/dev/nodejs/node-v18.16.0-linux-x64/bin /data/dev/nodejs/node-v18.16.0-linux-x64/lib/node_modules/pm2/bin/pm2 startup systemd -u taoyao --hp /home/taoyao
pm2 save

# 验证
pm2 conf
npm config get registry
pm2 -v
npm -v
node -v
```

## 安装Python

```
# 下载
mkdir -p /data/dev/python ; cd $_
#wget https://www.python.org/ftp/python/3.8.16/Python-3.8.16.tar.xz
wget https://mirrors.huaweicloud.com/python/3.8.16/Python-3.8.16.tar.xz
tar -Jxvf Python-3.8.16.tar.xz

# 安装
cd Python-3.8.16
./configure --with-ssl --enable-optimizations
make && sudo make install

# 配置
sudo ln -sf /usr/local/bin/pip3.8    /usr/local/bin/pip
sudo ln -sf /usr/local/bin/python3.8 /usr/local/bin/python

## 验证
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

## 安装ffmpeg

```
mkdir -p /data/dev/ffmpeg ; cd $_

# nasm
cd /data/dev/ffmpeg
wget https://www.nasm.us/pub/nasm/releasebuilds/2.16/nasm-2.16.tar.gz
tar -zxvf nasm-2.16.tar.gz
cd nasm-2.16/
./configure
make && sudo make install

# yasm
cd /data/dev/ffmpeg
wget https://www.tortall.net/projects/yasm/releases/yasm-1.3.0.tar.gz
tar -zxvf yasm-1.3.0.tar.gz
cd yasm-1.3.0/
./configure
make && sudo make install

# libvpx?  --enable-gpl --enable-libvpx
cd /data/dev/ffmpeg
#git clone https://chromium.googlesource.com/webm/libvpx.git
git clone https://github.com/webmproject/libvpx.git
cd libvpx/
git checkout v1.13.0
./configure --enable-static --enable-shared --enable-vp8 --enable-vp9 --enable-vp9-highbitdepth --as=yasm --disable-examples --disable-unit-tests
make && sudo make install

# libopus? --enable-gpl --enable-libopus
cd /data/dev/ffmpeg
wget https://archive.mozilla.org/pub/opus/opus-1.3.1.tar.gz
tar -zxvf opus-1.3.1.tar.gz
cd opus-1.3.1/
./configure --enable-static --enable-shared
make && sudo make install

# libx264? --enable-gpl --enable-libx264
cd /data/dev/ffmpeg
git clone https://code.videolan.org/videolan/x264.git
cd x264/
./configure --enable-static --enable-shared
make && sudo make install

# libx265? --enable-gpl --enable-libx265
cd /data/dev/ffmpeg
git clone https://bitbucket.org/multicoreware/x265_git
cd x265_git/
git checkout 3.5
cd build/linux/
cmake -G "Unix Makefiles" ../../source/
make && sudo make install

# ffmpeg
cd /data/dev/ffmpeg
wget http://www.ffmpeg.org/releases/ffmpeg-5.1.3.tar.xz
tar -Jxvf ffmpeg-5.1.3.tar.xz
cd ffmpeg-5.1.3/
PKG_CONFIG_PATH="/usr/local/lib/pkgconfig/"
./configure      \
--enable-static  \
--enable-shared  \
--enable-gpl     \
--enable-libvpx  \
--enable-libopus \
--enable-libx264 \
--enable-libx265 \
--enable-encoder=libvpx_vp8 --enable-decoder=vp8 --enable-parser=vp8 \
--enable-encoder=libvpx_vp9 --enable-decoder=vp9 --enable-parser=vp9
make && sudo make install

# 链接文件
vim /etc/ld.so.conf

---
/usr/local/lib/
---

ldconfig

# 验证
ffmpeg -version
ffmpeg -decoders
ffmpeg -encoders
```

## 安装Nginx

```
# 安装
sudo apt install nginx

# 配置自启
sudo systemctl enable|disable nginx

# 管理服务
sudo systemctl start|stop|restart nginx

# 加载配置
sudo nginx -s reload

# 配置用户
useradd -s /sbin/nologin -M nginx

# 验证
sudo nginx -V
```

## 下载源码

```
cd /data
git clone https://gitee.com/acgist/taoyao.git --recursive
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
sudo cp /data/taoyao/docs/etc/taoyao-signal-server.service /usr/lib/systemd/system/taoyao-signal-server.service

# 配置自启
sudo systemctl daemon-reload
sudo systemctl enable|disable taoyao-signal-server

# 执行脚本
./deploy.sh

# 管理服务
sudo systemctl start|stop|restart taoyao-signal-server
```

## 安装媒体

```
# 编译代码
cd /data/taoyao/taoyao-client-media
npm install

# 配置ecosystem
pm2 start|reload ecosystem.config.json
pm2 save

# 管理服务：服务名称必须和配置终端标识一致否则不能执行重启和关闭信令
pm2 start|stop|restart taoyao-client-media
```

### Mediasoup编译失败

编译过程中的依赖下载容易失败，需要进入目录`mediasoup/worker/subprojects`，查看`*.wrap`文件依次下载所需依赖，修改名称放到`packagefiles`目录中，最后注释下载链接。将`package.json`中的`mediasoup`改为本地依赖`file:./mediasoup`，重新编译即可。

> 下载依赖建议备份方便再次编译使用

### Mediasoup单独编译

编译媒体服务时会自动编译`mediasoup`所以忽略单独编译

```
# 编译代码
# make -C worker
cd /data/taoyao/taoyao-client-media/mediasoup/worker
make

# 清理结果
make clean
```

## 安装Web终端

`Nginx`和`PM2`选择一种启动即可

```
# 编译代码
cd /data/taoyao/taoyao-client-web
npm install

# 配置服务
pm2 start npm --name "taoyao-client-web" -- run dev
pm2 save

# 管理服务
pm2 start|stop|restart taoyao-client-web

# 打包代码
npm run build

# Nginx配置
sudo cp /data/taoyao/docs/etc/nginx.conf /etc/nginx/nginx.conf

sudo nginx -s reload
```

## 安装Android终端

```
cd /data/taoyao/taoyao-client-android/taoyao

# Mac | Linux
sh ./gradlew --no-daemon assembleRelease | installDebug | assembleDebug

# Windows
./gradlew.bat --no-daemon assembleRelease | installDebug | assembleDebug
```

## 防火墙

```
# 安装
apt install ufw

# 常用命令
sudo ufw status
sudo ufw enable|disable|reload

# 禁用所有
sudo ufw default deny
# SSH
sudo ufw allow ssh
# 终端服务（Web）：Nginx
sudo ufw allow 443/tcp
# 终端服务（Web）：PM2
sudo ufw allow 8443/tcp
# 信令服务（WebSocket）
sudo ufw allow 8888/tcp
# 信令服务（Socket）
sudo ufw allow 9999/tcp
# 媒体服务
sudo ufw allow 40000:49999/udp
# 允许网段
#sudo ufw allow from 192.168.1.0/24 to any

# 删除端口
#sudo ufw delete allow 443/tcp
#sudo ufw delete allow 8443/tcp
#sudo ufw delete allow 8888/tcp
#sudo ufw delete allow 9999/tcp
#sudo ufw delete allow 40000:49999/tcp
```

## 证书

```
mkdir /data/certs ; cd $_

# CA证书

openssl genrsa -out ca.key 2048
openssl req -x509 -new -key ca.key -out ca.crt -days 3650
openssl x509 -in ca.crt -subject -issuer -noout
# subject= /C=cn/ST=gd/L=gz/O=acgist/OU=acgist/CN=acgist.com
# issuer= /C=cn/ST=gd/L=gz/O=acgist/OU=acgist/CN=acgist.com

# Server证书信息

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

# Server证书

openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr
# 设置信息：-subj "/C=cn/ST=gd/L=gz/O=acgist/OU=taoyao/CN=taoyao.acgist.com"
openssl x509 -req -in server.csr -out server.crt -CA ca.crt -CAkey ca.key -CAcreateserial -days 3650 -extfile server.ext
openssl x509 -in server.crt -subject -issuer -noout
# subject= /C=cn/ST=gd/L=gz/O=acgist/OU=taoyao/CN=taoyao.acgist.com
# issuer= /C=cn/ST=gd/L=gz/O=acgist/OU=acgist/CN=acgist.com
openssl pkcs12 -export -clcerts -in server.crt -inkey server.key -out server.p12 -name taoyao
# 不要导出ca证书：-clcerts
# 设置密码：-passout pass:123456
# keytool -importkeystore -v -srckeystore server.p12 -srcstoretype pkcs12 -destkeystore server.jks -deststoretype jks
# 原始密码：-srcstorepass 123456
# 设置密码：-deststorepass 123456
```

## gcc/g++路径配置

```
# 安装路径
--prefix=/usr/local
--prefix=/usr/local/ffmpeg
# 执行文件路径
--bindir=/usr/local/bin
--bindir=/usr/local/ffmpeg/bin
# 库文件路径
--libdir=/usr/local/lib
--libdir=/usr/local/ffmpeg/lib
# 头文件路径
--includedir=/usr/local/include
--includedir=/usr/local/ffmpeg/include
```

## 清理源码

```
sudo rm -rf      \
/data/dev/cmake  \
/data/dev/ffmpeg \
/data/dev/python \
/data/dev/maven/apache-maven-3.8.8-bin.tar.gz   \
/data/dev/nodejs/node-v18.16.0-linux-x64.tar.xz \
/data/dev/java/openjdk-17.0.2_linux-x64_bin.tar.gz
```
