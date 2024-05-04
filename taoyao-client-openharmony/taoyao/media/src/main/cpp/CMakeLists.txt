cmake_minimum_required(VERSION 3.4.1)

project(taoyao VERSION 1.0.0 LANGUAGES C CXX)

# Debug | Release
#-DCMAKE_BUILD_TYPE=Debug
#set(CMAKE_BUILD_TYPE Debug)

# C编译选项
set(CMAKE_C_STANDARD 17)
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c17 -O3")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -std=c17 -O0 -g")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -std=c17 -O3")

# C++编译选项
set(CMAKE_CXX_STANDARD 17)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -O3")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -std=c++17 -O0 -g")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -std=c++17 -O3")

set(MEDIASOUPCLIENT_LOG_DEV     OFF)
set(MEDIASOUPCLIENT_LOG_TRACE   OFF)
set(MEDIASOUPCLIENT_BUILD_TESTS OFF)
set(LIBWEBRTC_BINARY_PATH  "${CMAKE_CURRENT_SOURCE_DIR}/deps/webrtc/lib/")
set(LIBWEBRTC_INCLUDE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/deps/webrtc/src/")

add_subdirectory("./deps/libmediasoupclient")

add_library(
    ${PROJECT_NAME} SHARED
    bind.cpp
    media/Room.cpp
    media/Client.cpp
    media/RoomClient.cpp
    media/LocalClient.cpp
    media/RemoteClient.cpp
    media/Player.cpp
    media/AudioPlayer.cpp
    media/VideoPlayer.cpp
    media/Capturer.cpp
    media/AudioCapturer.cpp
    media/VideoCapturer.cpp
    media/MediaManager.cpp
)

target_include_directories(
    ${PROJECT_NAME} PUBLIC
    "./include"
    "./deps/webrtc"
    "./deps/libmediasoupclient/include"
    "./deps/libmediasoupclient/deps/libsdptransform/include"
)

target_link_libraries(
    ${PROJECT_NAME} PUBLIC
    mediasoupclient
    libace_napi.z.so
    libhilog_ndk.z.so
)
