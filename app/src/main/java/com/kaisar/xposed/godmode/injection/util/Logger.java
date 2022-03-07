package com.kaisar.xposed.godmode.injection.util;

import android.util.Log;

import androidx.annotation.Keep;

/**
 * Created by jrsen on 17-10-21.
 * Use this command to enable logger "adb shell setprop log.tag.GodMode DEBUG"
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

    private final String mName;

    private Logger(String tag) {
        this.mName = tag;
    }

    public void d(String message) {
        if (isLoggable(TAG, Log.DEBUG)) d(TAG, String.format("[%s] %s", mName, message));
    }

    public void i(String message) {
        if (isLoggable(TAG, Log.INFO)) i(TAG, String.format("[%s] %s", mName, message));
    }

    public void w(String message) {
        if (isLoggable(TAG, Log.WARN)) w(TAG, String.format("[%s] %s", mName, message));
    }

    public void w(String message, Throwable tr) {
        if (isLoggable(TAG, Log.WARN)) w(TAG, String.format("[%s] %s", mName, message), tr);
    }

    public void e(String message) {
        if (isLoggable(TAG, Log.ERROR)) e(mName, String.format("[%s] %s", mName, message));
    }

    public void e(String message, Throwable tr) {
        if (isLoggable(TAG, Log.ERROR)) e(mName, String.format("[%s] %s", mName, message), tr);
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

}
