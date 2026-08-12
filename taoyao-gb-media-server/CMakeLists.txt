cmake_minimum_required(VERSION 3.22.0)

project(taoyao-gb-media-server VERSION 1.0.0 LANGUAGES C CXX)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

if(CMAKE_HOST_UNIX)
elseif(CMAKE_HOST_WIN32)
    add_definitions(-DOS_WINDOWS)
    if(MSVC)
        set(CMAKE_C_STANDARD 17)
        set(CMAKE_CXX_STANDARD 20)
        set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS} /std:c17 /std:c++20")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /std:c17 /std:c++20")
        add_compile_options("/Zc:__cplusplus")
        # /MD  MSVCRT.LIB   多线程DLL的Release版本
        # /MDd MSVCRTD.LIB  多线程DLL的Debug版本
        # /MT  LIBCMT.LIB   多线程静态链接的Release版本
        # /MTd LIBCMTD.LIB  多线程静态链接的Debug版本
        # /MD  MSVCPRT.LIB  多线程DLL的Release版本
        # /MDd MSVCPRTD.LIB 多线程DLL的Debug版本
        # /MT  LIBCPMT.LIB  多线程静态链接的Release版本
        # /MTd LIBCPMTD.LIB 多线程静态链接的Debug版本
        add_compile_definitions(_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR=1)
        if(CMAKE_BUILD_TYPE MATCHES "Debug")
            set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /NODEFAULTLIB:MSVCRT")
            set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} /NODEFAULTLIB:MSVCRT")
        else()
            set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /NODEFAULTLIB:MSVCRTD")
            set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} /NODEFAULTLIB:MSVCRTD")
        endif()
    else()
        message(WARNING "需要安装MSVC")
    endif()
else()
    message(FATAL_ERROR "不支持的系统：${CMAKE_HOST_SYSTEM_NAME}")
endif()

if(CMAKE_HOST_UNIX)
set(JDK_PATH "/usr/lib/jvm/java-17-openjdk-amd64")
set(JVM_LIB ${JDK_PATH}/lib/server/libjvm.so)
set(JVM_INC ${JDK_PATH}/include ${JDK_PATH}/include/linux)
elseif(CMAKE_HOST_WIN32)
set(JDK_PATH C:/develop/java/jdk-17)
set(JVM_LIB ${JDK_PATH}/lib/jvm.lib)
set(JVM_INC ${JDK_PATH}/include ${JDK_PATH}/include/win32)
endif()

include_directories(
    media-server/librtp/include
    media-server/libmpeg/include
    ${JVM_INC}
)

file(GLOB_RECURSE RTP_FILES
    media-server/librtp/source/*.c
    media-server/librtp/rtpext/*.c
    media-server/librtp/payload/*.c
)

add_library(
    taoyao-gb-media-rtp
    ${RTP_FILES}
)

file(GLOB_RECURSE MPEG_FILES
    media-server/libmpeg/source/*.c
)

add_library(
    taoyao-gb-media-mpeg
    ${MPEG_FILES}
)

add_library(
    taoyao-gb-media-server SHARED
    src/gb-media-server.cpp
)
target_link_libraries(
    taoyao-gb-media-server
    taoyao-gb-media-rtp
    taoyao-gb-media-mpeg
    ${JVM_LIB}
)
if(CMAKE_HOST_WIN32)
target_compile_options(taoyao-gb-media-server PRIVATE "/utf-8")
endif()

add_executable(
    taoyao-gb-media-server-test
    src/gb-media-server.cpp
)
target_link_libraries(
    taoyao-gb-media-server-test
    taoyao-gb-media-rtp
    taoyao-gb-media-mpeg
    ${JVM_LIB}
)
if(CMAKE_HOST_WIN32)
target_compile_options(taoyao-gb-media-server-test PRIVATE "/utf-8")
endif()
