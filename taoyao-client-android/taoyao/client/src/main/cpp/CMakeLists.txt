cmake_minimum_required(VERSION 3.22.1)

project(taoyao VERSION 1.0.0 LANGUAGES C CXX)

# Debug | Release
# DCMAKE_BUILD_TYPE=Debug
# set(CMAKE_BUILD_TYPE Debug)

# C编译选项
set(CMAKE_C_STANDARD 17)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c17 -O3")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -std=c17 -O0 -g")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -std=c17 -O3")

# C++编译选项
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -O3")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -std=c++17 -O0 -g")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -std=c++17 -O3")

# 输出目录
# set(PROJECT_BINARY_DIR ${PROJECT_SOURCE_DIR}/build)

file(
    GLOB SOURCE_FILE
    "main.hpp"
    "include/*.hpp"
    "mediasoup/*.hpp"
    "main.cpp"
    "media/*.cpp"
)

add_library(taoyao SHARED ${SOURCE_FILE})
find_library(
    log-lib
    log
)
target_link_libraries(
    taoyao
    ${log-lib}
)

# 配置
# cmake -B build
# Windows：cmake -B build -G "Unix Makefiles"
# 编译
# cmake --build build
# 标准线程：-lpthread
