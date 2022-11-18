# 部署

## 源

```
cd /etc/yum.repos.d
rm -rf *
wget /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo
yum makecache
```

## 证书

```
keytool -genkeypair -keyalg RSA -dname "CN=localhost, OU=acgist, O=taoyao, L=GZ, ST=GD, C=CN" -alias taoyao -validity 3650 -ext ku:c=dig,keyE -ext eku=serverAuth -ext SAN=dns:localhost,ip:127.0.0.1 -keystore taoyao.jks -keypass 123456 -storepass 123456
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

## 应用

```
```

## 防火墙

```
firewall-cmd --zone=public --add-port=8888/tcp --permanent
firewall-cmd --zone=public --add-port=45535-65535/tcp --permanent
firewall-cmd --zone=public --add-port=45535-65535/udp --permanent

firewall-cmd --reload
firewall-cmd --list-ports
firewall-cmd --zone=public --remove-port=45535-65535/tcp --permanent
firewall-cmd --zone=public --remove-port=45535-65535/udp --permanent
```