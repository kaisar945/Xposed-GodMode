package com.kaisar.xposed.godmode.injection.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.kaisar.xposed.godmode.BuildConfig;

public class GmResources {

    private static Resources GMResources;

    private static Resources getGmResource(Context context) throws PackageManager.NameNotFoundException {
        if (GMResources == null) {
            GMResources = context.createPackageContext(BuildConfig.APPLICATION_ID, 0).getResources();
        }
        return GMResources;
    }

    public static int getColor(Context context, int id) throws Resources.NotFoundException {
        try {
            return getGmResource(context).getColor(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public static Drawable getDrawable(Context context, int id) throws Resources.NotFoundException {
        try {
            return getGmResource(context).getDrawable(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    public static CharSequence getText(Context context, int id) throws Resources.NotFoundException {
        try {
            return getGmResource(context).getText(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    public static String getString(Context context, int id) throws Resources.NotFoundException {
        try {
            return getGmResource(context).getString(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    public static String getString(Context context, int id, Object... formatArgs) throws Resources.NotFoundException {
        try {
            return getGmResource(context).getString(id, formatArgs);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }
}
