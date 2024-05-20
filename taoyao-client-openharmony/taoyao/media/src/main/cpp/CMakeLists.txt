cmake_minimum_required(VERSION 3.4.1)

project(taoyao VERSION 1.0.0 LANGUAGES C CXX)

# Debug | Release
#-DCMAKE_BUILD_TYPE=Debug
#set(CMAKE_BUILD_TYPE Debug)

# C编译选项
set(CMAKE_C_STANDARD 17)
#set(CMAKE_C_FLAGS        "${CMAKE_C_FLAGS}         -fno-rtti -std=c17 -O3")
set(CMAKE_C_FLAGS_DEBUG   "${CMAKE_C_FLAGS_DEBUG}   -fno-rtti -std=c17 -O0 -g")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -fno-rtti -std=c17 -O3")

# C++编译选项
set(CMAKE_CXX_STANDARD 17)
#set(CMAKE_CXX_FLAGS        "${CMAKE_CXX_FLAGS}         -fno-rtti -std=c++17 -O3")
set(CMAKE_CXX_FLAGS_DEBUG   "${CMAKE_CXX_FLAGS_DEBUG}   -fno-rtti -std=c++17 -O0 -g")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -fno-rtti -std=c++17 -O3")

set(LIBWEBRTC_BINARY_PATH  "${CMAKE_CURRENT_SOURCE_DIR}/deps/webrtc/lib/${OHOS_ARCH}/")
set(LIBWEBRTC_INCLUDE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/deps/webrtc/src/")

#ADD_DEFINITIONS(-DNDEBUG)
ADD_DEFINITIONS(-DLOG_TAG="libtaoyao")
ADD_DEFINITIONS(-DVK_USE_PLATFORM_OHOS=1)

add_subdirectory("./deps/libmediasoupclient")

add_library(
    ${PROJECT_NAME} SHARED
    bind.cpp
    media/Signal.cpp
    media/WebRTC.cpp
    media/Room.cpp
    media/Client.cpp
    media/RoomClient.cpp
    media/LocalClient.cpp
    media/RemoteClient.cpp
    media/MediaManager.cpp
    media/Player.cpp
    media/AudioPlayer.cpp
    media/VideoPlayer.cpp
    media/VideoDecoder.cpp
    media/Capturer.cpp
    media/VideoEncoder.cpp
    media/AudioCapturer.cpp
    media/VideoCapturer.cpp
)

target_include_directories(
    ${PROJECT_NAME} PUBLIC
    "./include"
    "./deps/webrtc/src"
    "./deps/libmediasoupclient/include"
    "./deps/libmediasoupclient/deps/libsdptransform/include"
)

target_link_libraries(
    ${PROJECT_NAME} PUBLIC
    # libuv
    libuv.so
    # EGL
    libEGL.so
    # OpenGL ES
    libGLESv3.so
    # 音频
    libohaudio.so
    # 相机
    libohcamera.so
    # NAPI
    libace_napi.z.so
    # LOG
    libhilog_ndk.z.so
    # 图片
    libnative_image.so
    # NativeBuffer
    libnative_buffer.so
    # NativeWindow
    libnative_window.so
    # 编码解码
    libnative_media_aenc.so
    libnative_media_venc.so
    libnative_media_core.so
    libnative_media_codecbase.so
    # libmediasoupclient
    mediasoupclient
)
