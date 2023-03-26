cmake_minimum_required(VERSION 3.22.1)

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

set(
    SOURCE_DIR
    src/main/cpp/
)

set(
    SOURCE_FILES
    ${SOURCE_DIR}/rtp.hpp
    ${SOURCE_DIR}/rtp.cpp
    ${SOURCE_DIR}/webrtc.hpp
    ${SOURCE_DIR}/webrtc.cpp
)

set(LIBWEBRTC_BINARY_PATH ${LIBWEBRTC_BINARY_PATH}/${ANDROID_ABI} CACHE STRING "libwebrtc binary path" FORCE)

if (${MEDIASOUPCLIENT_LOG_TRACE})
    target_compile_definitions(
        ${PROJECT_NAME} PRIVATE MSC_LOG_TRACE=1
    )
endif ()

if (${MEDIASOUPCLIENT_LOG_DEV})
    target_compile_definitions(
        ${PROJECT_NAME} PRIVATE MSC_LOG_DEV=1
    )
endif ()

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

add_subdirectory("deps/libmediasoupclient")

set_source_files_properties(
    ${SOURCE_FILES} PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic
)

target_include_directories(
    ${PROJECT_NAME} PUBLIC
    "${SOURCE_DIR}/include"
    "${PROJECT_SOURCE_DIR}/deps/libmediasoupclient/include"
    "${PROJECT_SOURCE_DIR}/deps/libmediasoupclient/deps/libsdptransform/include"
)

target_link_libraries(
    ${PROJECT_NAME} PUBLIC
    log
    android
    OpenSLES
    mediasoupclient
)
