/**
 * IO Hook - Stub implementation
 *
 * Future: Will intercept open/openat syscalls for virtual filesystem redirection.
 */

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "GachaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_gachacloudloader_native_IoHook_nativeInit(JNIEnv *env, jobject thiz) {
    LOGI("IoHook nativeInit called (stub)");
}

} // extern "C"
