cmake_minimum_required(VERSION 3.28)
project(magica)

set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -Werror")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Werror")

add_library(libcap STATIC
    libcap/cap_alloc.c
    libcap/cap_extint.c
    libcap/cap_file.c
    libcap/cap_flag.c
    libcap/cap_proc.c
    libcap/cap_text.c
)

target_include_directories(libcap PUBLIC
    libcap/include
)

target_compile_options(libcap PRIVATE
    -Wall -Wextra -Werror -Wno-unused-parameter -Wno-sign-compare
)

set(LSPLT_BUILD_SHARED OFF CACHE BOOL "Build lsplt as static library only")
add_subdirectory(external/lsplt/lsplt/src/main/jni)

add_library(magica SHARED
    magica.cpp
)

target_include_directories(magica PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(magica
    libcap
    lsplt_static
    log
)
