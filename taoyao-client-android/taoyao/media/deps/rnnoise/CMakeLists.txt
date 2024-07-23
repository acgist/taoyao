cmake_minimum_required(VERSION 3.22.1)

project(rnnoise VERSION 1.0.0 LANGUAGES C)

set(CMAKE_C_STANDARD 17)
set(CMAKE_C_FLAGS_DEBUG   "${CMAKE_C_FLAGS_DEBUG}   -std=c17 -O0 -g")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -std=c17 -O3")

include_directories(include)

set(
    SOURCE_FILES
    src/rnn.c
    src/nnet.c
    src/pitch.c
    src/denoise.c
    src/celt_lpc.c
    src/kiss_fft.c
    src/nnet_default.c
    src/rnnoise_tables.c
    src/rnnoise_data.c
#   src/rnnoise_data_little.c
    src/parse_lpcnet_weights.c
)

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

set_source_files_properties(
    ${SOURCE_FILES} PROPERTIES COMPILE_FLAGS -Wall -Wextra -Wpedantic
)
