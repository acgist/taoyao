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

set(LIBWEBRTC_INCLUDE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/deps/webrtc/src")
set(LIBWEBRTC_BINARY_PATH  "${CMAKE_CURRENT_SOURCE_DIR}/deps/webrtc/lib/arm64-v8a")

add_subdirectory("./deps/libmediasoupclient")

add_library(
    ${PROJECT_NAME} SHARED
    bind.cpp
    media/Room.cpp
    media/Client.cpp
    media/LocalClient.cpp
    media/RemoteClient.cpp
    media/MediaManager.cpp
    media/SessionClient.cpp
)

target_include_directories(
    ${PROJECT_NAME} PUBLIC
    "./include"
    "./deps/libmediasoupclient/include"
    "./deps/libmediasoupclient/deps/libsdptransform/include"
)

target_link_libraries(
    ${PROJECT_NAME} PUBLIC
    mediasoupclient
    libace_napi.z.so
)
