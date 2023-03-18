cmake_minimum_required(VERSION 3.2)

#define CMAKE_TEMPLATE_VERSION_MAJOR @CMakeTemplate_VERSION_MAJOR@
#define CMAKE_TEMPLATE_VERSION_MINOR @CMakeTemplate_VERSION_MINOR@
#define CMAKE_TEMPLATE_VERSION_PATCH @CMakeTemplate_VERSION_PATCH@
project(taoyao-client-camera VERSION 1.0.0 LANGUAGES C CXX)

# Debug | Release
# DCMAKE_BUILD_TYPE=Debug
set(CMAKE_BUILD_TYPE Debug)

# set(CMAKE_C_COMPILER gcc)
# set(CMAKE_CXX_COMPILER g++)

# 全局宏定义
# add_definitions(-DDEBUG)

# 全局编译选项
# add_compile_options()

# C编译选项
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c11 -O3")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -g -O0")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3")

# C++编译选项
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -O0")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3")

# 输出目录
set(PROJECT_BINARY_DIR ${PROJECT_SOURCE_DIR}/build)

# 包含源代码
# add_subdirectory(src)
# add_subdirectory(test)

file(
  GLOB SOURCE_FILE
  "include/*.h"
  "include/*.hpp"
  "src/main.cpp"
  "src/media/*.c"
  "src/signal/*.c"
  "src/media/*.cpp"
  "src/signal/*.cpp"
)

# -lpthread

add_executable(${PROJECT_NAME} ${SOURCE_FILE})

# message("源码地址：" ${SOURCE_FILE})

# 配置
# cmake -B build
# Windows：-G "Unix Makefiles"
# 安装位置：-DCMAKE_INSTALL_PREFIX=`pwd`/output | --install --prefix <install-path>
# 打包位置：-DCPACK_OUTPUT_FILE_PREFIX=`pwd`/output

# 编译
# cmake --build build

# 安装
# cd build && make install && cd -

# install(
#   TARGETS taoyao
#   RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
#   LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
#   ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
# )

# 打包
# cd build && cpack && cd -

# include(CPack)
# set(CPACK_GENERATOR "ZIP")
# set(CPACK_SET_DESTDIR ON)
# set(CPACK_INSTALL_PREFIX "")
# set(CPACK_PACKAGE_NAME "taoyao")
# set(CPACK_PACKAGE_VERSION ${PROJECT_VERSION})
