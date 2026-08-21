/**
 * FileSystem Hook - Stub implementation
 *
 * Future: Will provide filesystem redirection hooks for NewBlackbox integration.
 */

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "GachaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_gachacloudloader_native_FileSystemHook_nativeInit(JNIEnv *env, jobject thiz) {
    LOGI("FileSystemHook nativeInit called (stub)");
}

} // extern "C"
