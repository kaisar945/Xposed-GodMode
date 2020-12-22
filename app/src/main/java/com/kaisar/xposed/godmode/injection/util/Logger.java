package com.kaisar.xposed.godmode.injection.util;

import androidx.annotation.Keep;
import android.util.Log;

/**
 * Created by jrsen on 17-10-21.
 */

@Keep
public final class Logger {

    private static final String TAG = "GodMode";

    public static int v(String tag, String msg) {
        return isLoggable(tag, Log.VERBOSE) ? Log.v(tag, msg) : 0;
    }

    public static int v(String tag, String msg, Throwable tr) {
        return isLoggable(tag, Log.VERBOSE) ? Log.v(tag, msg, tr) : 0;
    }

    public static int d(String tag, String msg) {
        return isLoggable(tag, Log.DEBUG) ? Log.d(tag, msg) : 0;
    }

    public static int d(String tag, String msg, Throwable tr) {
        return isLoggable(tag, Log.DEBUG) ? Log.d(tag, msg, tr) : 0;
    }

    public static int i(String tag, String msg) {
        return isLoggable(tag, Log.INFO) ? Log.i(tag, msg) : 0;
    }

    public static int i(String tag, String msg, Throwable tr) {
        return isLoggable(tag, Log.INFO) ? Log.i(tag, msg, tr) : 0;
    }

    public static int w(String tag, String msg) {
        return isLoggable(tag, Log.WARN) ? Log.w(tag, msg) : 0;
    }

    public static int w(String tag, String msg, Throwable tr) {
        return isLoggable(tag, Log.WARN) ? Log.w(tag, msg, tr) : 0;
    }

    public static int e(String tag, String msg) {
        return isLoggable(tag, Log.ERROR) ? Log.e(tag, msg) : 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        return isLoggable(tag, Log.ERROR) ? Log.e(tag, msg, tr) : 0;
    }

    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    public static boolean isLoggable(String tag, int level) {
        return Log.isLoggable(TAG, level) || Log.isLoggable(tag, level);
    }

}
