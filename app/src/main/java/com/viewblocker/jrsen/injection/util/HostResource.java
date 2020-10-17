package com.viewblocker.jrsen.injection.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.viewblocker.jrsen.BuildConfig;

public class HostResource {

    private static android.content.res.Resources hostResources;

    public static void initResource(Context context) throws PackageManager.NameNotFoundException {
        if (hostResources == null) {
            Context packageContext = context.createPackageContext(BuildConfig.APPLICATION_ID, 0);
            hostResources = packageContext.getResources();
        }
    }

    @Deprecated
    public static int getColor(int id) throws Resources.NotFoundException {
        return hostResources.getColor(id);
    }

    @Deprecated
    public static Drawable getDrawable(int id) throws Resources.NotFoundException {
        return hostResources.getDrawable(id);
    }

}
