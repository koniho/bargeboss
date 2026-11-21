#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
# On-device development uses Termux's Android compiler and glslang. Header
# dependencies stay outside source control; packaging includes its C++ runtime.
if [ -n "${TERMUX_VERSION:-}" ] || [ "$(uname -o 2>/dev/null)" = Android ]; then
    headers="${BARGEBOSS_VULKAN_HEADERS:-${PREFIX:?}/include}"
    [ -f "$headers/vulkan/vulkan.h" ] || { echo 'Set BARGEBOSS_VULKAN_HEADERS to a Vulkan-Headers include directory.' >&2; exit 1; }
    mkdir -p build/android/vulkan/assets/shaders build/android/vulkan/lib/arm64-v8a
    for shader in scene.vert scene.frag flat.vert; do
        glslang -V -Os --target-env vulkan1.0 -Iassets/shaders "android/native/$shader" -o "build/android/vulkan/assets/shaders/$shader.spv"
    done
    clang++ -std=c++17 -O2 -fPIC -shared -Wall -Wextra -Werror -I"$headers" \
        -Wno-missing-field-initializers -Wl,-z,max-page-size=16384 -Wl,-rpath,'$ORIGIN' \
        android/native/VulkanRenderer.cpp -L/system/lib64 -lvulkan -landroid -ljnigraphics -llog \
        -o build/android/vulkan/lib/arm64-v8a/libbargeboss_vulkan.so
    cp "$PREFIX/lib/libc++_shared.so" build/android/vulkan/lib/arm64-v8a/
    mkdir -p build/android/vulkan/assets/licenses
    cp docs/licenses/libcxx-LICENSE.txt build/android/vulkan/assets/licenses/
    exit 0
fi
ndk="${ANDROID_NDK_HOME:-${ANDROID_HOME:?Set ANDROID_HOME}/ndk/27.0.12077973}"
host=darwin-x86_64
case "$(uname -s)" in Linux) host=linux-x86_64;; esac
bin="$ndk/toolchains/llvm/prebuilt/$host/bin"
mkdir -p build/android/vulkan/assets/shaders build/android/vulkan/lib/arm64-v8a
rm -f build/android/vulkan/lib/arm64-v8a/libc++_shared.so
for stage in vert frag; do
 "$ndk/shader-tools/$host/glslc" -O -Iassets/shaders --target-env=vulkan1.0 "android/native/scene.$stage" -o "build/android/vulkan/assets/shaders/scene.$stage.spv"
done
"$ndk/shader-tools/$host/glslc" -O --target-env=vulkan1.0 android/native/flat.vert -o build/android/vulkan/assets/shaders/flat.vert.spv
"$bin/aarch64-linux-android24-clang++" -std=c++17 -O2 -fPIC -shared -static-libstdc++ -Wall -Wextra -Werror \
 -Wno-missing-field-initializers -Wl,-z,max-page-size=16384 android/native/VulkanRenderer.cpp \
 -lvulkan -landroid -ljnigraphics -llog -o build/android/vulkan/lib/arm64-v8a/libbargeboss_vulkan.so
