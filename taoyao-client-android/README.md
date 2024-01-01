# 安卓终端

## 支持版本

* SDK     28~32
* Gradle  7.5
* Andoird 9~12

## C++终端

* [libmediasoupclient源码](https://github.com/versatica/libmediasoupclient)
* [libmediasoupclient文档](https://mediasoup.org/documentation/v3/libmediasoupclient)
* [libmediasoupclient接口](https://mediasoup.org/documentation/v3/libmediasoupclient/api)

## 项目配置

可以自己编译`WebRTC`依赖或者下载已有依赖，项目导入以后拷贝`libmediasoupclient`源码还有`WebRTC`头文件和库文件到`deps`目录。

[WebRTC](https://pan.baidu.com/s/1E_DXv32D9ODyj5J-o-ji_g?pwd=hudc)

> 注意删除目录`build`目录和`third_party`目录中除了`abseil-cpp`以外的所有依赖（当然不删也没关系就是文件太多编译器会变慢）

## 录像优化

改为`surface`实现性能更高

## 视频旋转

1. 应用旋转：横屏竖屏
2. 物理旋转：旋转镜头

## SDK源码修改

由于原始SDK部分功能没有提供，所以修改了部分SDK，如果需要升级项目注意同步修改，修改文件列表：

* JavaAudioDeviceModule
* WebRtcAudioRecord
* WebRtcAudioTrack

## 学习资料

* https://developer.android.google.cn/docs?hl=zh-cn
* https://developer.android.google.cn/guide?hl=zh-cn

## 依赖编译（可选）

* https://webrtc.github.io/webrtc-org/native-code/android/
* https://webrtc.github.io/webrtc-org/native-code/development/
* https://webrtc.github.io/webrtc-org/native-code/development/prerequisite-sw/
* https://www.chromium.org/developers/how-tos/install-depot-tools/
* https://mediasoup.org/documentation/v3/libmediasoupclient/installation/

## 安卓编译（可选）

* https://mirrors.tuna.tsinghua.edu.cn/help/AOSP/
* https://source.android.google.cn/source/initializing?hl=zh-cn

## 参考项目

* https://github.com/haiyangwu/webrtc-android-build
* https://github.com/haiyangwu/mediasoup-demo-android
* https://github.com/haiyangwu/mediasoup-client-android

## 现有实现

* `org.webrtc:google-webrtc`
* `io.github.haiyangwu:mediasoup-client`

## YUV

```
Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y
Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y
Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y
Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y      Y Y Y Y Y Y
U U U U U U      V V V V V V      U V U V U V      V U V U V U
V V V V V V      U U U U U U      U V U V U V      V U V U V U
- I420 -          - YV12 -         - NV12 -         - NV21 -

I420 = YUV420P = YU12
NV12 = YUV420SP

RGB和YUV转换算法：BT.601（标清）、BT.709（高清）、BT.2020（超高清）
```
