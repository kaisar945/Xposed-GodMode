package com.kaisar.xposed.godmode.injection.hook;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.Property;

import java.util.Optional;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

/**
 * 不知道为什么在Android Q中并未发现DisplayProperties代码很奇怪
 */
public final class DisplayPropertiesHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {

    private boolean mDebugLayout;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        if (mDebugLayout) {
            param.setResult(Optional.of(true));
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