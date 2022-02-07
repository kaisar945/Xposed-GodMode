package com.kaisar.xposed.godmode.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Keep;

import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;

/**
 * Created by jrsen on 17-11-22.
 */
@Keep
public final class XposedEnvironment {

    public enum XposedType {
        XPOSED("de.robv.android.xposed.installer"),
        EDXPOSED("org.meowcat.edxposed.manager"),
        TAICHI("me.weishu.exp"),
        UNKNOWN("unknown");

        public final String PACKAGE_NAME;

        XposedType(String packageName) {
            PACKAGE_NAME = packageName;
        }
    }

    public static XposedType checkXposedType(Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm.getLaunchIntentForPackage(XposedType.XPOSED.PACKAGE_NAME) != null) {
            return XposedType.XPOSED;
        } else if (pm.getLaunchIntentForPackage(XposedType.EDXPOSED.PACKAGE_NAME) != null) {
            return XposedType.EDXPOSED;
        } else if (pm.getLaunchIntentForPackage(XposedType.TAICHI.PACKAGE_NAME) != null) {
            return XposedType.TAICHI;
        } else {
            return XposedType.UNKNOWN;
        }
    }

    public static boolean isModuleActive(Context context) {
        if (GodModeManager.getDefault().getStatus()) return true;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
            Bundle result = null;
            try {
                result = contentResolver.call(uri, "active", null, null);
            } catch (RuntimeException e) {
                // TaiChi is killed, try invoke
                try {
                    Intent intent = new Intent("me.weishu.exp.ACTION_ACTIVE");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Throwable e1) {
                    return false;
                }
            }
            if (result == null) {
                result = contentResolver.call(uri, "active", null, null);
            }

            if (result == null) {
                return false;
            }
            return result.getBoolean("active", false);
        } catch (Throwable ignored) {
        }
        return false;
    }

}
