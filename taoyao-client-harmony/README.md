# 鸿蒙终端

提供嵌入式开发能力，选择开发板`Hi3518E`。

## C++终端

* [libmediasoupclient源码](https://github.com/versatica/libmediasoupclient)
* [libmediasoupclient文档](https://mediasoup.org/documentation/v3/libmediasoupclient)
* [libmediasoupclient接口](https://mediasoup.org/documentation/v3/libmediasoupclient/api)

## 学习资料

https://hpm.harmonyos.com/#/cn/home
https://repo.harmonyos.com/#/cn/home
https://www.hihope.org/download/download.aspx
https://device.harmonyos.com/cn/develop/ide/
https://device.harmonyos.com/cn/documentation/
https://www.openharmony.cn/docs/zh-cn/overview/
https://www.openharmony.cn/docs/zh-cn/device-dev/
https://docs.openharmony.cn/pages/v3.1/zh-cn/OpenHarmony-Overview_zh.md/
https://docs.openharmony.cn/pages/v3.1/zh-cn/device-dev/device-dev-guide.md/
https://docs.openharmony.cn/pages/v3.1/zh-cn/device-dev/hpm-part/hpm-part-about.md/

## 环境

```
# Ubuntu
# https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/20.04.5/

# `Ubuntu`选择版本`20.04.5`，建议选择相同版本，不然很多软件版本需要自己调整。

# 环境搭建
# https://device.harmonyos.com/cn/docs/documentation/guide/ide-install-windows-ubuntu-0000001194073744
```

## 源码

```
# 下载源码
# https://gitee.com/openharmony/manifest/tree/OpenHarmony-3.2-Release/
# https://gitee.com/openharmony/docs/blob/master/zh-cn/device-dev/get-code/sourcecode-acquire.md

repo init -u https://gitee.com/openharmony/manifest.git -b OpenHarmony-3.2-Release -m chipsets/hispark_aries.xml -g ohos:mini --no-repo-verify
repo sync -c
repo forall -c 'git lfs pull'

# Marketplace
# https://hpm.harmonyos.com/#/cn/solution/@ohos%2Fhispark_aries
# https://repo.harmonyos.com/#/cn/solution/@opensource%2Fhoperun_hm_door_3518
# https://gitee.com/openharmony-sig/knowledge_demo_smart_home/blob/master/dev/docs/smart_door_viewer_3518/README.md

sudo apt install nodejs
sudo apt install npm
npm config set registry https://repo.huaweicloud.com/repository/npm
npm install -g @ohos/hpm-cli
```

## 编译

https://hpm.harmonyos.com/#/cn/home
https://docs.openharmony.cn/pages/v3.1/zh-cn/device-dev/hpm-part/hpm-part-about.md/

```
# 下载依赖
hpm install

# hpm编译
hpm dist

# hb编译
pip3 install build/lite
# hp增量编译
hb build
# hp完整编译
hb build -f

# build编译
./build.sh \
    --ccache \
    --product-name Hi3518EV300 \
    --build-target build_kernel \
    --gn-args linux_kernel_version=\"linux-5.10\" 
```

## 烧录

https://www.hihope.org/download/download.aspx

```
# out/hispark_aries/ipcamera_hispark_aries
# device/hisilicon/hispark_aries/sdk_liteos/uboot/out/boot

u-boot-hi3518ev300.bin：fastboot
OHOS_Image.bin：kernel
rootfs_jffs2.img：rootfs
userfs_jffs2.img：userfs
```

## 配置

串口：`115200`

```
setenv bootcmd "sf probe 0;sf read 0x40000000 0x100000 0x600000;go 0x40000000";
setenv bootargs "console=ttyAMA0,115200n8 root=flash fstype=jffs2 rw rootaddr=7M rootsize=8M";
save;
reset
./bin/wpa_supplicant -iwlan0 -c /etc/wpa_supplicant.conf
```

## 代码目录

```
applications    应用程序样例，包括camera等
base            基础软件服务子系统集&硬件服务子系统集
build           组件化编译、构建和配置脚本
docs            说明文档
domains         增强软件服务子系统集
drivers         驱动子系统
foundation      系统基础能力子系统集
kernel          内核子系统
prebuilts       编译器及工具链子系统
test            测试子系统
third_party     开源第三方组件
utils           常用的工具集
vendor          厂商提供的软件
build.py        编译脚本文件
```
