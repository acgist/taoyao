# 桃夭终端

## 支持版本

* SDK 9

## C++终端

* [libmediasoupclient源码](https://github.com/versatica/libmediasoupclient)
* [libmediasoupclient文档](https://mediasoup.org/documentation/v3/libmediasoupclient)
* [libmediasoupclient接口](https://mediasoup.org/documentation/v3/libmediasoupclient/api)

## 项目配置

* https://gitee.com/openharmony-sig/ohos_webrtc/blob/master/doc/webrtc_build.md

```
# WebRTC版本：m114
# libmediasoupclient版本：m120

gn gen ./out/ohos_webrtc --args='target_os="ohos" target_cpu="arm64" is_clang=true is_debug=false use_rtti=true rtc_use_h264=true use_custom_libcxx=false rtc_include_tests=false is_component_build=false treat_warnings_as_errors=false rtc_build_examples=false libyuv_include_tests=false rtc_use_dummy_audio_file_devices=true ohos_sdk_native_root="/data/dev/ohos-sdk/linux/native"'

ninja -C ./out/ohos_webrtc -j 32
```

## openharmony-sig/ohos_webrtc

* https://gitee.com/openharmony-sig/ohos_webrtc
* https://gitee.com/openharmony-sig/ohos_webrtc/tree/master/doc

## openharmony-tpc/chromium_third_party_webrtc

* https://gitee.com/openharmony-tpc
* https://gitee.com/openharmony-tpc/chromium_third_party_webrtc
* https://gitee.com/openharmony-tpc/chromium_third_party_ohos_prebuilts

## Linux（鸿蒙）

```
# 编译工具
mkdir -p /data
cd /data
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git

# 下载源码
mkdir -p /data/webrtc
cd /data/webrtc
/data/depot_tools/fetch --nohooks webrtc
#/data/depot_tools/gclient sync

# 切换分支
cd src
git checkout -b m94 branch-heads/4606
/data/depot_tools/gclient sync

# 编译依赖
./build/install-build-deps.sh

# 鸿蒙工具
wget https://repo.huaweicloud.com/openharmony/os/4.0-Release/ohos-sdk-windows_linux-public.tar.gz

# 编译项目
./buildtools/linux64/gn gen out/Release-clang-x64 --args='target_os="linux" target_cpu="x64" is_clang=true is_debug=false use_rtti=true rtc_use_h264=true use_custom_libcxx=true rtc_include_tests=false is_component_build=false treat_warnings_as_errors=false rtc_build_examples=false'
./third_party/depot_tools/ninja -C out/Release-clang-x64

./buildtools/linux64/gn gen out/Release-clang-x86 --args='target_os="linux" target_cpu="x86" is_clang=true is_debug=false use_rtti=true rtc_use_h264=true use_custom_libcxx=true rtc_include_tests=false is_component_build=false treat_warnings_as_errors=false rtc_build_examples=false'
./third_party/depot_tools/ninja -C out/Release-clang-x86

./build/linux/sysroot_scripts/install-sysroot.py --arch=arm
./buildtools/linux64/gn gen out/Release-clang-arm --args='target_os="linux" target_cpu="arm" is_clang=true is_debug=false use_rtti=true rtc_use_h264=true use_custom_libcxx=true rtc_include_tests=false is_component_build=false treat_warnings_as_errors=false rtc_build_examples=false'
./third_party/depot_tools/ninja -C out/Release-clang-arm

./build/linux/sysroot_scripts/install-sysroot.py --arch=arm64
./buildtools/linux64/gn gen out/Release-clang-arm64 --args='target_os="linux" target_cpu="arm64" is_clang=true is_debug=false use_rtti=true rtc_use_h264=true use_custom_libcxx=true rtc_include_tests=false is_component_build=false treat_warnings_as_errors=false rtc_build_examples=false'
./third_party/depot_tools/ninja -C out/Release-clang-arm64
```

# 国产

> https://blog.csdn.net/oqqKuKu12/article/details/136029483

## aarch64

```
# 安装交叉编译工具链
sudo apt-get install binutils-aarch64-linux-gnu
sudo apt-get install gcc-7-aarch64-linux-gnu
sudo apt-get install g++-7-aarch64-linux-gnu
cd /usr/bin
sudo ln -s aarch64-linux-gnu-gcc-7 aarch64-linux-gnu-gcc
sudo ln -s aarch64-linux-gnu-g++-7 aarch64-linux-gnu-g++

# 源码安装交叉工具依赖库
cd webrtc/build/linux/sysroot_scripts
./install-sysroot.py --arch=arm64

# 交叉编译
gn gen out/Release-aarch64 --args='
      target_os="linux"
      target_cpu="arm64"
      is_clang=false
      is_debug=false
      use_rtti=true
      rtc_use_h264=true
      use_custom_libcxx=false
      rtc_include_tests=false
      is_component_build=false
      treat_warnings_as_errors=false
      rtc_build_examples=false
      symbol_level=0
      rtc_use_x11=true
      rtc_build_ssl=true
      rtc_build_tools=false
      rtc_use_pipewire=true
      rtc_enable_protobuf=false
      rtc_include_pulse_audio=false
      ffmpeg_branding="Chrome"
      proprietary_codecs=true
      use_partition_alloc=false
      '

ninja -C out/Release-aarch64
```

## mips64el

```
# 安装交叉编译工具链
sudo apt-get install binutils-mips64el-linux-gnu
sudo apt-get install gcc-7-mips64el-linux-gnu
sudo apt-get install g++-7-mips64el-linux-gnu
cd /usr/bin
sudo ln -s mips64el-linux-gnu-gcc-7 mips64el-linux-gnu-gcc
sudo ln -s mips64el-linux-gnu-g++-7 mips64el-linux-gnu-g++

# 源码安装交叉工具依赖库
cd webrtc/build/linux/sysroot_scripts
./install-sysroot.py --arch=mips64el

# 交叉编译
gn gen out/Release-mips64el --args='
      target_os="linux"
      target_cpu="mips64el"
      is_clang=false
      is_debug=false
      use_rtti=true
      rtc_use_h264=true
      use_custom_libcxx=false
      rtc_include_tests=false
      is_component_build=false
      treat_warnings_as_errors=false
      rtc_build_examples=false
      symbol_level=0
      rtc_use_x11=true
      rtc_build_ssl=true
      rtc_build_tools=false
      rtc_use_pipewire=true
      rtc_enable_protobuf=false
      ffmpeg_branding="Chrome"
      proprietary_codecs=true
      use_partition_alloc=false
      '

ninja -C out/Release-mips64el
```

## loongarch64

```
# 安装交叉编译工具链
cd /opt/cross_compile/loongarch64
wget http://ftp.loongnix.cn/toolchain/gcc/release/loongarch/gcc8/loongson-gnu-toolchain-8.3-x86_64-loongarch64-linux-gnu-rc1.2.tar.xz
xz loongson-gnu-toolchain-8.3-x86_64-loongarch64-linux-gnu-rc1.2.tar.xz

# 配置WebRTC添加平台支持
cd build/toolchain/linux
vim BUILD.gn
gcc_toolchain("loongarch64") {
  toolprefix = "loongarch64-linux-gnu-"
  cc = "${toolprefix}gcc -w"
  cxx= "${toolprefix}g++ -w"
  ar = "${toolprefix}ar"
  ld = cxx
  readelf = "${toolprefix}readelf"
  nm = "${toolprefix}nm"

  toolchain_args = {
    cc_wrapper = ""
    current_cpu = "loongarch64"
    current_os = "linux"
    is_clang = false
    use_goma = false
  }
}

# 配置环境和sysroot
vim ~/.profile
export PATH=$PATH:/opt/cross_compile/loongarch64/usr/bin
export SYSROOT="/opt/cross_compile/loongarch64/usr/sysroot"
export CPATH=$CPATH:${SYSROOT}/usr/include:${SYSROOT}/usr/include/glib-2.0:${SYSROOT}/usr/lib/glib-2.0/include:${SYSROOT}/usr/include/gio-unix-2.0
. ~/.profile

      target_os="linux"
      target_cpu="mips64el"
      is_clang=false
      is_debug=false
      use_rtti=true
      rtc_use_h264=true
      use_custom_libcxx=false
      rtc_include_tests=false
      is_component_build=false
      treat_warnings_as_errors=false
      rtc_build_examples=false
      symbol_level=0
      rtc_use_x11=true
      rtc_build_ssl=true
      rtc_build_tools=false
      rtc_use_pipewire=true
      rtc_enable_protobuf=false
      ffmpeg_branding="Chrome"
      proprietary_codecs=true
      use_partition_alloc=false
      '

# 交叉编译
gn gen out/Release-loongarch64 --args='
      target_os="linux"
      target_cpu="loongarch64"
      is_clang=false
      is_debug=false
      use_rtti=true
      rtc_use_h264=true
      use_custom_libcxx=false
      rtc_include_tests=false
      is_component_build=false
      treat_warnings_as_errors=false
      rtc_build_examples=false
      symbol_level=0
      rtc_use_x11=true
      rtc_build_ssl=true
      rtc_build_tools=false
      rtc_use_pipewire=false
      rtc_enable_protobuf=false
      ffmpeg_branding="Chrome"
      proprietary_codecs=true
      use_partition_alloc=false
      '

ninja -C out/Release-loongarch64
```

# Build command for all platforms

> https://github.com/webrtc-sdk/webrtc-build/blob/main/docs/build.md

## iOS arm

```bash
gn gen out/ios-arm-device --args="
      target_os = \"ios\"
      ios_enable_code_signing = false
      use_xcode_clang = true
      is_component_build = false
      target_environment = \"device\"
      target_cpu = \"arm\"
      ios_deployment_target = \"10.0\"
      enable_ios_bitcode = false
      use_goma = false
      rtc_enable_symbol_export = true
      rtc_libvpx_build_vp9 = true
      rtc_include_tests = false
      rtc_build_examples = false
      rtc_use_h264 = false
      rtc_enable_protobuf = false
      use_rtti = true
      is_debug = false
      enable_dsyms = false
      enable_stripping = true"

ninja -C out/ios-arm-device ios_framework_bundle
```

## iOS arm64

```bash
gn gen out/ios-arm64-device --args="
      target_os = \"ios\"
      ios_enable_code_signing = false
      use_xcode_clang = true
      is_component_build = false
      target_environment = \"device\"
      target_cpu = \"arm64\"
      ios_deployment_target = \"10.0\"
      enable_ios_bitcode = false
      use_goma = false
      rtc_enable_symbol_export = true
      rtc_libvpx_build_vp9 = true
      rtc_include_tests = false
      rtc_build_examples = false
      rtc_use_h264 = false
      rtc_enable_protobuf = false
      use_rtti = true
      is_debug = false
      enable_dsyms = false
      enable_stripping = true"

ninja -C out/ios-arm64-device ios_framework_bundle
```

## iOS x64 simulator

```bash
gn gen out/ios-x64-simulator --args="
      target_os = \"ios\"
      ios_enable_code_signing = false
      use_xcode_clang = true
      is_component_build = false
      target_environment = \"simulator\"
      target_cpu = \"x64\"
      ios_deployment_target = \"12.0\"
      rtc_libvpx_build_vp9 = true
      enable_ios_bitcode = false
      use_goma = false
      rtc_enable_symbol_export = true
      rtc_libvpx_build_vp9 = true
      rtc_include_tests = false
      rtc_build_examples = false
      rtc_use_h264 = false
      rtc_enable_protobuf = false
      use_rtti = true
      is_debug = false
      enable_dsyms = false
      enable_stripping = true"

ninja -C out/ios-x64-simulator ios_framework_bundle
```

## iOS arm64 simulator

```bash
gn gen out/ios-arm64-simulator --args="
      target_os = \"ios\"
      ios_enable_code_signing = false
      use_xcode_clang = true
      is_component_build = false
      target_environment = \"simulator\"
      target_cpu = \"arm64\"
      ios_deployment_target = \"12.0\"
      enable_ios_bitcode = false
      use_goma = false
      rtc_enable_symbol_export = true
      rtc_libvpx_build_vp9 = true
      rtc_include_tests = false
      rtc_build_examples = false
      rtc_use_h264 = false
      rtc_enable_protobuf = false
      use_rtti = true
      is_debug = false
      enable_dsyms = false
      enable_stripping = true"

ninja -C out/ios-arm64-simulator ios_framework_bundle
```

## macOS X64

```bash
gn gen out/macOS-x64 --args="
      target_os=\"mac\"
      target_cpu=\"x64\"
      use_xcode_clang = false
      mac_deployment_target=\"10.11\"
      is_component_build = false
      target_cpu = \"x64\"
      use_goma = false
      rtc_enable_symbol_export = true
      rtc_libvpx_build_vp9 = true
      rtc_include_tests = false
      rtc_build_examples = false
      rtc_use_h264 = false
      rtc_enable_protobuf = false
      use_rtti = true
      is_debug = false
      enable_dsyms = false
      enable_stripping = true"

ninja -C out/macOS-x64 mac_framework_bundle
```

## macOS arm64

```bash
gn gen out/macOS-arm64 --args="
      target_os=\"mac\"
      target_cpu=\"x64\"
      use_xcode_clang = false
      mac_deployment_target=\"10.11\"
      is_component_build = false
      target_cpu = \"arm64\"
      use_goma = false
      rtc_enable_symbol_export = true
      rtc_libvpx_build_vp9 = true
      rtc_include_tests = false
      rtc_build_examples = false
      rtc_use_h264 = false
      rtc_enable_protobuf = false
      use_rtti = true
      is_debug = false
      enable_dsyms = false
      enable_stripping = true"

ninja -C out/macOS-arm64 mac_framework_bundle
```

## Create xcframework

Merge the arm64 and x64 libraries of macOS.

```bash
mkdir -p out/mac-x64-arm64-lib
cp -R out/macOS-x64/WebRTC.framework out/mac-x64-arm64-lib/WebRTC.framework
lipo -create -output out/mac-x64-arm64-lib/WebRTC.framework/WebRTC out/macOS-x64/WebRTC.framework/WebRTC out/macOS-arm64/WebRTC.framework/WebRTC
```

Merge the arm64 and arm libraries of iOS.

```bash
mkdir -p out/ios-device-arm-arm64-lib
cp -R out/ios-arm64-device/WebRTC.framework out/ios-device-arm-arm64-lib/WebRTC.framework
lipo -create -output out/ios-device-arm-arm64-lib/WebRTC.framework/WebRTC out/ios-arm-device/WebRTC.framework/WebRTC out/ios-arm64-device/WebRTC.framework/WebRTC
```

Merge the arm64 and x64 libraries of iOS simulator.

```bash
mkdir -p out/ios-simulator-arm64-x64-lib
cp -R out/ios-arm64-simulator/WebRTC.framework out/ios-simulator-arm64-x64-lib/WebRTC.framework
lipo -create -output out/ios-simulator-arm64-x64-lib/WebRTC.framework/WebRTC out/ios-arm64-simulator/WebRTC.framework/WebRTC out/ios-x64-simulator/WebRTC.framework/WebRTC
```

Create xcframework

```bash
xcodebuild -create-xcframework \
        -framework out/ios-device-arm-arm64-lib/WebRTC.framework \
        -framework out/ios-simulator-arm64-x64-lib/WebRTC.framework \
        -framework out/mac-x64-arm64-lib/WebRTC.framework \
        -output out/WebRTC.xcframework
cp LICENSE out/WebRTC.xcframework/
```

Fix symbolic links issue for macOS

```
cd out/WebRTC.xcframework/macos-arm64_x86_64/WebRTC.framework/
mv WebRTC Versions/A/WebRTC
ln -s Versions/Current/WebRTC WebRTC
```

Create a release zip file

```
cd out/
zip --symlinks -9 -r WebRTC.xcframework.zip WebRTC.xcframework
# hash
shasum -a 256 WebRTC.xcframework.zip
```

## Android

```bash
vpython ./tools_webrtc/android/build_aar.py --build-dir webrtc_android --output ./webrtc_android/libwebrtc.aar --arch armeabi-v7a arm64-v8a x86_64 x86 --extra-gn-args 'is_java_debug=false rtc_include_tests=false rtc_use_h264=false is_component_build=false use_rtti=true rtc_build_examples=false treat_warnings_as_errors=false'
```

## Linux

```bash
gn gen out/Linux-x64 --args="target_os=\"linux\" target_cpu=\"x64\" is_debug=false rtc_include_tests=false rtc_use_h264=false is_component_build=false use_rtti=true use_custom_libcxx=false rtc_enable_protobuf=false"
gn gen out/Linux-x86 --args="target_os=\"linux\" target_cpu=\"x86\" is_debug=false rtc_include_tests=false rtc_use_h264=false is_component_build=false use_rtti=true use_custom_libcxx=false rtc_enable_protobuf=false"
```

## Linux ARM/ARM64

```bash
gn gen out/Linux-arm --args="target_os=\"linux\" target_cpu=\"arm\" is_debug=false rtc_include_tests=false rtc_use_h264=false is_component_build=false use_rtti=true use_custom_libcxx=false rtc_enable_protobuf=false"
gn gen out/Linux-arm64 --args="target_os=\"linux\" target_cpu=\"arm64\" is_debug=false rtc_include_tests=false rtc_use_h264=false is_component_build=false use_rtti=true use_custom_libcxx=false rtc_enable_protobuf=false"

```

## Windows

```console
set DEPOT_TOOLS_WIN_TOOLCHAIN=0
gn gen out/Windows-x64 --args="target_os=\"win\" target_cpu=\"x64\" is_debug=false rtc_include_tests=false rtc_use_h264=true ffmpeg_branding=\"Chrome\"  is_component_build=false use_rtti=true use_custom_libcxx=false rtc_enable_protobuf=false"
gn gen out/Windows-x86 --args="target_os=\"win\" target_cpu=\"x86\" is_debug=false rtc_include_tests=false rtc_use_h264=true ffmpeg_branding=\"Chrome\"  is_component_build=false use_rtti=true use_custom_libcxx=false rtc_enable_protobuf=false"
```
