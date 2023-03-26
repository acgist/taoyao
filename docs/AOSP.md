# AOSP

本文档内容旨在独立定制编译`AOSP`系统，非必需使用。

## 参考文档

* https://mirrors.tuna.tsinghua.edu.cn/help/AOSP/
* https://source.android.google.cn/source/initializing?hl=zh-cn
* https://source.android.google.cn/docs/setup/about/build-numbers?hl=zh-cn
* https://developers.google.cn/android/drivers#sargosp2a.220505.008

## 机器配置

* 内存`32G`
* 十六核`CPU`
* 硬盘`300G`
* 系统`Ubuntu 18.xx`
* 公司网络`100Mbps/s`
* 整个下载过程大概需要四到五个小时
* 整个编译过程大概需要半到一个小时

## 源码

```
# 工具
mkdir /data/android
cd /data/android
curl https://mirrors.tuna.tsinghua.edu.cn/git/git-repo -o repo
chmod a+x repo
export REPO_URL='https://mirrors.tuna.tsinghua.edu.cn/git/git-repo'

git config --global user.email "taoyao@acgist.com"
git config --global user.name "acgist"

# 源码
./repo init -u https://mirrors.tuna.tsinghua.edu.cn/git/AOSP/platform/manifest -b android-12.1.0_r27
./repo sync
```

ROOT权限

## 裁剪

```
# 应用

# 驱动

# root
userdebug

# framework
```

## 编译

```
lunch aosp_arm64-user
make -j 8
```

## 刷机

```
adb reboot bootloader
fastboot flashall
```


https://blog.csdn.net/u012932409/article/details/106792906
https://blog.csdn.net/qq_33240707/article/details/123704679
https://blog.csdn.net/weixin_42929891/article/details/122667831