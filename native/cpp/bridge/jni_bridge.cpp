/**
 * JNI Bridge - Stub implementation
 *
 * Future: Will provide JNI bridge for BEnvironment/IOCore/NativeCore integration.
 */

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "GachaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_gachacloudloader_native_NativeBridge_getVersion(JNIEnv *env, jclass clazz) {
    LOGI("NativeBridge getVersion called (stub)");
    return env->NewStringUTF("0.1.0-stub");
}

JNIEXPORT void JNICALL
Java_com_example_gachacloudloader_native_NativeBridge_nativeInit(JNIEnv *env, jclass clazz) {
    LOGI("NativeBridge nativeInit called (stub)");
}

} // extern "C"
