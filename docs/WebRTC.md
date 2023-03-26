# WebRTC

本文档内容旨在独立编译`WebRTC`项目，非必需使用。

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
* 宽带按需`100Mbps/s`（不要固定宽带）
* 整个下载过程大概需要半到一个小时
* 整个编译过程大概需要一到两个小时
* 最痛苦的就是下载回来速度很慢

```
# 编译工具
mkdir -p /data
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git

# 源码
mkdir -p /data/webrtc
cd /data/webrtc
fetch --nohooks webrtc_android
/data/depot_tools/gclient sync

# 分支
cd src
git checkout -b m94 branch-heads/4606
/data/depot_tools/gclient sync

# 编译依赖
./build/install-build-deps.sh
./build/install-build-deps-android.sh
source ./build/android/envsetup.sh

# 编译配置：./tools_webrtc/android/build_aar.py
---
'target_os': 'android',
'is_clang': True,
'is_debug': False,
'use_rtti': True,
'rtc_use_h264': True,
'use_custom_libcxx': False,
'rtc_include_tests': False,
'is_component_build': False,
'treat_warnings_as_errors': False,
'use_goma': use_goma,
'target_cpu': _GetTargetCpu(arch)
---

# 编译项目
./tools_webrtc/android/build_aar.py --build-dir ./out/release-build/
# 指定CPU架构：--arch x86 x86_64 arm64-v8a armeabi-v7a

# 生成静态库
/data/depot_tools/autoninja -C ./out/release-build/x86 webrtc
/data/depot_tools/autoninja -C ./out/release-build/x86_64 webrtc
/data/depot_tools/autoninja -C ./out/release-build/arm64-v8a webrtc
/data/depot_tools/autoninja -C ./out/release-build/armeabi-v7a webrtc

# 打包
zip -r webrtc.zip out libwebrtc.aar
```

[WebRTC](https://pan.baidu.com/s/1E_DXv32D9ODyj5J-o-ji_g?pwd=hudc)

## libmediasoupclient

https://mediasoup.org/documentation/v3/libmediasoupclient/installation/

```
# 编译
cmake . -B build \
-DCMAKE_BUILD_TYPE=Debug | Release \
-DMEDIASOUPCLIENT_LOG_DEV=OFF \
-DMEDIASOUPCLIENT_LOG_TRACE=OFF \
-DMEDIASOUPCLIENT_BUILD_TESTS=OFF \
-DLIBWEBRTC_INCLUDE_PATH:PATH=PATH_TO_LIBWEBRTC_SOURCES \
-DLIBWEBRTC_BINARY_PATH:PATH=PATH_TO_LIBWEBRTC_BINARY
make -C build
make install -C build
```
