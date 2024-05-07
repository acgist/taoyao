# 桃夭终端

## 支持版本

* SDK 11

## C++终端

* [libmediasoupclient源码](https://github.com/versatica/libmediasoupclient)
* [libmediasoupclient文档](https://mediasoup.org/documentation/v3/libmediasoupclient)
* [libmediasoupclient接口](https://mediasoup.org/documentation/v3/libmediasoupclient/api)

## 项目配置

可以自己编译`WebRTC`依赖或者下载已有依赖，项目导入以后拷贝`libmediasoupclient`源码还有`WebRTC`头文件和库文件到`deps`目录。

[WebRTC](https://pan.baidu.com/s/1E_DXv32D9ODyj5J-o-ji_g?pwd=hudc)

> 注意删除目录`build`目录和`third_party`目录中除了`abseil-cpp`以外的所有依赖（当然不删也没关系就是文件太多编译器会变慢）

* https://gitee.com/openharmony-sig/ohos_webrtc/blob/master/doc/webrtc_build.md

## 鸿蒙编译

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
