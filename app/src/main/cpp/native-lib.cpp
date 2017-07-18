#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_pw_ethan_lib_util_NativeKitUtil_StringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
