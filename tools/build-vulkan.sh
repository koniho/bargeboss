#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
ndk="${ANDROID_NDK_HOME:-${ANDROID_HOME:?Set ANDROID_HOME}/ndk/27.0.12077973}"
host=darwin-x86_64
case "$(uname -s)" in Linux) host=linux-x86_64;; esac
bin="$ndk/toolchains/llvm/prebuilt/$host/bin"
mkdir -p build/android/vulkan/assets/shaders build/android/vulkan/lib/arm64-v8a
for stage in vert frag; do
 "$ndk/shader-tools/$host/glslc" -O -Iassets/shaders --target-env=vulkan1.0 "android/native/scene.$stage" -o "build/android/vulkan/assets/shaders/scene.$stage.spv"
done
"$bin/aarch64-linux-android24-clang++" -std=c++17 -O2 -fPIC -shared -static-libstdc++ -Wall -Wextra -Werror \
 -Wno-missing-field-initializers -Wl,-z,max-page-size=16384 android/native/VulkanRenderer.cpp \
 -lvulkan -landroid -ljnigraphics -llog -o build/android/vulkan/lib/arm64-v8a/libbargeboss_vulkan.so
