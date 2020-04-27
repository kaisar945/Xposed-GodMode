package com.viewblocker.jrsen.injection.util;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.IInterface;

import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public final class PackageManagerUtils {

    private static IInterface packageService;

    private static Object getIPackageManager() {
        // TODO: 19-11-10 9.0系统可能存在兼容性的问题
        if (packageService == null) {
            try {
                @SuppressLint("PrivateApi") Class<?> ServiceManagerClass = Class.forName("android.os.ServiceManager");
                IBinder binder = (IBinder) XposedHelpers.callStaticMethod(ServiceManagerClass, "checkService", "package");
                @SuppressLint("PrivateApi") Class<?> IPackageManager$StubClass = Class.forName("android.content.pm.IPackageManager$Stub");
                packageService = (IInterface) XposedHelpers.callStaticMethod(IPackageManager$StubClass, "asInterface", binder);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return packageService;
    }

    @SuppressWarnings("unchecked")
    public static List<ResolveInfo> queryIntentServices(Intent intent, String type, int flags, int userId) {
        Object parceledList = XposedHelpers.callMethod(getIPackageManager(), "queryIntentServices", intent, type, flags, userId);
        if (parceledList == null) {
            return Collections.emptyList();
        }
        return (List<ResolveInfo>) XposedHelpers.callMethod(parceledList, "getList");
    }

    public static ResolveInfo resolveIntent(Intent intent, String type, int flags, int userId) {
        return (ResolveInfo) XposedHelpers.callMethod(getIPackageManager(), "resolveIntent", intent, type, flags, userId);
    }

    public static ResolveInfo resolveService(Intent intent, String type, int flags, int userId) {
        return (ResolveInfo) XposedHelpers.callMethod(getIPackageManager(), "resolveService", intent, type, flags, userId);
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        return (PackageInfo) XposedHelpers.callMethod(getIPackageManager(), "getPackageInfo", packageName, flags, userId);
    }

}
