cmake_minimum_required(VERSION 3.22.1)

project("gachacloudloader-native" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android NDK configuration
if(ANDROID)
    set(CMAKE_ANDROID_NDK ${CMAKE_ANDROID_NDK} CACHE PATH "Android NDK path")
    set(ANDROID_PLATFORM ${ANDROID_PLATFORM} CACHE STRING "Android API level")
    set(CMAKE_ANDROID_ARCH_ABI ${ANDROID_ABI} CACHE STRING "Target ABI")
endif()

# Architecture: NDK integration points for NewBlackbox
# Currently a skeleton - actual implementation depends on NewBlackbox research
# Target areas for future implementation:
# - IO redirection hooks (open/openat interception)
# - FileSystemHook bridges
# - UnixFileSystemHook integration
# - JNI bridge for BEnvironment/IOCore/NativeCore

add_library(
    gachacloudloader-native
    SHARED
    cpp/io/io_hook.cpp
    cpp/hook/filesystem_hook.cpp
    cpp/bridge/jni_bridge.cpp
)

target_include_directories(
    gachacloudloader-native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/cpp
)

target_link_libraries(
    gachacloudloader-native
    PRIVATE
    android
    log
)

# NewBlackbox native library dependencies will be added here after research
# target_link_libraries(gachacloudloader-native PRIVATE newblackbox_native)