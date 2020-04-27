package com.viewblocker.jrsen.injection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.BuildConfig;
import com.viewblocker.jrsen.injection.bridge.ClientReceiver;
import com.viewblocker.jrsen.injection.bridge.LocalInjectBridge;
import com.viewblocker.jrsen.injection.hook.ActivityLifecycleHook;
import com.viewblocker.jrsen.injection.hook.DispatchTouchEventHook;
import com.viewblocker.jrsen.injection.hook.DisplayProperties;
import com.viewblocker.jrsen.injection.hook.SystemPropertiesHook;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.injection.util.PackageManagerUtils;
import com.viewblocker.jrsen.injection.util.Property;
import com.viewblocker.jrsen.rule.ActRules;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.viewblocker.jrsen.BlockerApplication.TAG;


/**
 * Created by jrsen on 17-10-13.
 */

public final class BlockerInjector implements IXposedHookLoadPackage {

    public static Property<Boolean> switchProp = new Property<>();
    public static Property<ActRules> actRuleProp = new Property<>();
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!loadPackageParam.isFirstApplication || checkWhiteListPackage(loadPackageParam.packageName)) {
            return;
        }
        Logger.i(TAG, "inject package:" + loadPackageParam.packageName + " isFirstApplication:" + loadPackageParam.isFirstApplication);
        BlockerInjector.loadPackageParam = loadPackageParam;
        if (BuildConfig.APPLICATION_ID.equals(loadPackageParam.packageName)) {
            //检测上帝模式模块是否开启
            XposedHelpers.findAndHookMethod("com.viewblocker.jrsen.util.XposedEnvironment", loadPackageParam.classLoader, "isModuleActive", Context.class, XC_MethodReplacement.returnConstant(true));
        } else {
            // TODO: 19-11-12 检测宿主应用目录权限
            initHook(loadPackageParam.classLoader);
            LocalInjectBridge injectBridge = LocalInjectBridge.initialize(loadPackageParam.packageName);
            injectBridge.registerReceiver(loadPackageParam.packageName, new ClientReceiver());
            switchProp.set(injectBridge.isInEditMode());
            actRuleProp.set(injectBridge.getRules(loadPackageParam.packageName));
        }
    }

    private boolean checkWhiteListPackage(String packageName) {
        if ("android".equals(packageName) || "com.android.systemui".equals(packageName)) {
            return true;
        }
        try {
            //检查是否为launcher应用
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = PackageManagerUtils.resolveIntent(homeIntent, null, PackageManager.MATCH_DEFAULT_ONLY, 0);
            Logger.i(TAG, "default launcher:" + ((resolveInfo != null) ? resolveInfo.activityInfo.packageName : "null"));
            if (resolveInfo != null && resolveInfo.activityInfo != null
                    && TextUtils.equals(resolveInfo.activityInfo.packageName, packageName)) {
                return true;
            }

            //检查是否为键盘应用
            Intent keyboardIntent = new Intent("android.view.InputMethod");
            List<ResolveInfo> resolveInfoList;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                resolveInfoList = PackageManagerUtils.queryIntentServices(keyboardIntent, null, PackageManager.MATCH_ALL, 0);
            } else {
                resolveInfoList = PackageManagerUtils.queryIntentServices(keyboardIntent, null, PackageManager.MATCH_DEFAULT_ONLY, 0);
            }
            Logger.i(TAG, "default keyboard:" + ((resolveInfo != null) ? resolveInfoList : "null"));
            for (ResolveInfo info : resolveInfoList) {
                if (TextUtils.equals(info.serviceInfo.packageName, packageName)) {
                    return true;
                }
            }

            //检查是否为无界面应用
            PackageInfo packageInfo = PackageManagerUtils.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES, 0);
            return packageInfo != null && packageInfo.activities != null && packageInfo.activities.length == 0;
        } catch (Throwable t) {
            Logger.e(TAG, "checkWhiteListPackage crash", t);
        }
        return false;
    }

    private void initHook(ClassLoader classLoader) {
        //hook activity#lifecycle block view
        ActivityLifecycleHook lifecycleHook = new ActivityLifecycleHook();
        actRuleProp.addOnPropertyChangeListener(lifecycleHook);
        XposedHelpers.findAndHookMethod(Activity.class, "onPostResume", lifecycleHook);
        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", lifecycleHook);

        //hook debug layout
        if (Build.VERSION.SDK_INT < 29) {
            SystemPropertiesHook systemPropertiesHook = new SystemPropertiesHook();
            switchProp.addOnPropertyChangeListener(systemPropertiesHook);
            XposedHelpers.findAndHookMethod("android.os.SystemProperties", classLoader, "getBoolean", String.class, boolean.class, systemPropertiesHook);
        } else {
            DisplayProperties displayPropertiesHook = new DisplayProperties();
            switchProp.addOnPropertyChangeListener(displayPropertiesHook);
            XposedHelpers.findAndHookMethod("android.sysprop.DisplayProperties", classLoader, "debug_layout", displayPropertiesHook);
        }
        //Disable show layout margin bound
        XposedHelpers.findAndHookMethod(ViewGroup.class, "onDebugDrawMargins", Canvas.class, Paint.class, XC_MethodReplacement.DO_NOTHING);

        //Drag view support
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent", MotionEvent.class, new DispatchTouchEventHook());
    }

}
