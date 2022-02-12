package com.kaisar.xposed.godmode.injection.util;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.kaisar.xposed.godmode.injection.GodModeInjector;

public class GmResources {

    private static Resources getGmResource() throws PackageManager.NameNotFoundException {
        return GodModeInjector.moduleRes;
    }

    public static int getColor(int id) throws Resources.NotFoundException {
        try {
            return getGmResource().getColor(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public static Drawable getDrawable(int id) throws Resources.NotFoundException {
        try {
            return getGmResource().getDrawable(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    public static CharSequence getText(int id) throws Resources.NotFoundException {
        try {
            return getGmResource().getText(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    public static String getString(int id) throws Resources.NotFoundException {
        try {
            return getGmResource().getString(id);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }

    public static String getString(int id, Object... formatArgs) throws Resources.NotFoundException {
        try {
            return getGmResource().getString(id, formatArgs);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Resources.NotFoundException("get resources fail GodMode package may be not installed?");
        }
    }
}
