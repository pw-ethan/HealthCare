package com.pw.ethan.lib.util;

public class NativeKitUtil {
    static {
        System.loadLibrary("native-lib");
    }
    public static native String StringFromJNI();
}
