# WebRTC

本文档内容旨在独立编译`WebRTC`项目，并非必需使用。

## libwebrtc

* https://webrtc.github.io/webrtc-org/native-code/android/
* https://webrtc.github.io/webrtc-org/native-code/development/
* https://webrtc.github.io/webrtc-org/native-code/development/prerequisite-sw/
* https://www.chromium.org/developers/how-tos/install-depot-tools/

国内镜像需要配置比较麻烦，建议直接按需购买能够访问外网的主机，用完直接释放，配置建议：

* 内存`8G`
* 四核`CPU`
* 硬盘`100G`
* 系统`Ubuntu 20.xx`
* 宽带按需`100MB/s`
* 整个下载过程大概需要半到一个小时
* 整个编译过程大概需要一到两个小时

## 代码编译

```
# 编译工具
mkdir -p /data
cd /data
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git

# 下载源码
mkdir -p /data/webrtc
cd /data/webrtc
/data/depot_tools/fetch --nohooks webrtc_android
#/data/depot_tools/gclient sync

# 切换分支
cd src
git checkout -b m94 branch-heads/4606
/data/depot_tools/gclient sync

# 编译依赖
./build/install-build-deps.sh
./build/install-build-deps-android.sh
source ./build/android/envsetup.sh

# 编译配置：./tools_webrtc/android/build_aar.py
---
'target_os'               : 'android',
'is_clang'                : True,
'is_debug'                : False,
'use_rtti'                : True,
'rtc_use_h264'            : True,
'use_custom_libcxx'       : False,
'rtc_include_tests'       : False,
'is_component_build'      : False,
'treat_warnings_as_errors': False,
'use_goma'                : use_goma,
'target_cpu'              : _GetTargetCpu(arch)
---

# 编译项目
./tools_webrtc/android/build_aar.py --build-dir ./out/release-build/ --arch x86 x86_64 arm64-v8a armeabi-v7a

# 安装ninja
apt install ninja-build

# 环境变量
PATH=$PATH:/data/webrtc/src/third_party/depot_tools/

# 生成静态库
cd /data/webrtc/src
/data/depot_tools/autoninja -C ./out/release-build/x86         webrtc &&
/data/depot_tools/autoninja -C ./out/release-build/x86_64      webrtc &&
/data/depot_tools/autoninja -C ./out/release-build/arm64-v8a   webrtc &&
/data/depot_tools/autoninja -C ./out/release-build/armeabi-v7a webrtc

# 打包文件
zip -r lib.zip out libwebrtc.aar

# 提取源代码
zip -r java.zip                                                                             \
sdk/android/api/                                                                            \
sdk/android/src/                                                                            \
rtc_base/java/src/                                                                          \
modules/audio_device/android/java/src/                                                      \
out/release-build/arm64-v8a/gen/sdk/android/video_api_java/generated_java/input_srcjars/    \
out/release-build/arm64-v8a/gen/sdk/android/peerconnection_java/generated_java/input_srcjars/

# 提取头文件
mkdir src
vim header.sh
---
#!/bin/bash
 
src=`find ./ -name "*.h"`
for header in $src
do
    echo "cp header file $header"
    cp --parents $header src
done

src=`find ./ -name "*.hpp"`
for header in $src
do
    echo "cp header file $header"
    cp --parents $header src
done
---
sh header.sh
zip -r src.zip src
```

[WebRTC](https://pan.baidu.com/s/1E_DXv32D9ODyj5J-o-ji_g?pwd=hudc)

## libmediasoupclient

https://mediasoup.org/documentation/v3/libmediasoupclient/installation/

```
# 编译
cmake . -B build                                        \
-DCMAKE_BUILD_TYPE=Debug | Release                      \
-DMEDIASOUPCLIENT_LOG_DEV=OFF                           \
-DMEDIASOUPCLIENT_LOG_TRACE=OFF                         \
-DMEDIASOUPCLIENT_BUILD_TESTS=OFF                       \
-DLIBWEBRTC_BINARY_PATH:PATH=PATH_TO_LIBWEBRTC_BINARY   \
-DLIBWEBRTC_INCLUDE_PATH:PATH=PATH_TO_LIBWEBRTC_SOURCES
make -C build
make install -C build
```
