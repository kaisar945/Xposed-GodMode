package com.kaisar.xposed.godmode.injection.hook;


import android.annotation.SuppressLint;

import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.Property;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

public final class SystemPropertiesHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {

    private boolean mDebugLayout;

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        if (mDebugLayout && "debug.layout".equals(param.args[0])) {
            param.setResult(true);
        }
    }

    @Override
    public void onPropertyChange(Boolean debugLayout) {
        mDebugLayout = debugLayout;
        try {
            @SuppressLint("PrivateApi") Class<?> SystemPropertiesClass = Class.forName("android.os.SystemProperties");
            XposedHelpers.callStaticMethod(SystemPropertiesClass, "callChangeCallbacks");
        } catch (ClassNotFoundException e) {
            Logger.e(TAG, "invoke callChangeCallbacks fail", e);
        }
    }
}