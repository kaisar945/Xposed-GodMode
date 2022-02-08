package com.kaisar.xposed.godmode.injection.hook;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.Property;

import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class DisplayPropertiesHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {

    private final HashMap<View, Drawable> view = new HashMap<>(); // TODO: 危险的写法， 需要找到合适的时机清空HashMap

    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        view.put((View) param.thisObject, null);
    }

    @Override
    public void onPropertyChange(Boolean debugLayout) {
        boolean mDebugLayout = debugLayout;
        if (mDebugLayout) {
            addViewShape(view);
        } else {
            delViewShape(view);
        }
        try {
            @SuppressLint("PrivateApi") Class<?> SystemPropertiesClass = Class.forName("android.os.SystemProperties");
            XposedHelpers.callStaticMethod(SystemPropertiesClass, "callChangeCallbacks");
        } catch (ClassNotFoundException e) {
            Logger.e(TAG, "invoke callChangeCallbacks fail", e);
        }
    }

    private void addViewShape(HashMap<View, Drawable> view) {
        for (View thisView : view.keySet()) {
            try {
                if (thisView.getVisibility() == View.VISIBLE) { // 只给可见view染色
                    GradientDrawable gd = new GradientDrawable();
                    gd.setStroke(2, Color.RED);
                    view.put(thisView, thisView.getBackground());
                    thisView.setBackground(gd);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void delViewShape(HashMap<View, Drawable> view) {
        for (View thisView : view.keySet()) {
            try {
                thisView.setBackground(view.get(thisView));
            } catch (Throwable ignored) {}
        }
    }
}