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
import android.os.IBinder;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.BuildConfig;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.injection.bridge.ManagerObserver;
import com.viewblocker.jrsen.injection.hook.ActivityLifecycleHook;
import com.viewblocker.jrsen.injection.hook.DispatchTouchEventHook;
import com.viewblocker.jrsen.injection.hook.DisplayProperties;
import com.viewblocker.jrsen.injection.hook.SystemPropertiesHook;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.injection.util.PackageManagerUtils;
import com.viewblocker.jrsen.injection.util.Property;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.service.GodModeManagerService;
import com.viewblocker.jrsen.service.XServiceManager;

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
        if (!loadPackageParam.isFirstApplication) {
            return;
        }
        BlockerInjector.loadPackageParam = loadPackageParam;
        final String packageName = loadPackageParam.packageName;
        switch (packageName) {
            case "android": {
                // 注册系统服务
                Logger.i(TAG, "inject GodModeManagerService as system service.");
                XServiceManager.registerService("godmode", new XServiceManager.ServiceFetcher<IBinder>() {
                    @Override
                    public IBinder createService(Context ctx) {
                        return new GodModeManagerService();
                    }
                });
                XServiceManager.initManager();
            }
            break;
            case BuildConfig.APPLICATION_ID: {
                //检测上帝模式模块是否开启
                XposedHelpers.findAndHookMethod("com.viewblocker.jrsen.util.XposedEnvironment", loadPackageParam.classLoader, "isModuleActive", Context.class, XC_MethodReplacement.returnConstant(true));
            }
            break;
            default: {
                if (checkBlockList(loadPackageParam.packageName)) {
                    Logger.i(TAG, String.format("%s in block list.", loadPackageParam.packageName));
                    return;
                }
                initHook(loadPackageParam.classLoader);
                GodModeManager manager = GodModeManager.getDefault();
                manager.addObserver(loadPackageParam.packageName, new ManagerObserver());
                switchProp.set(manager.isInEditMode());
                actRuleProp.set(manager.getRules(loadPackageParam.packageName));
            }
            break;
        }
    }

    private boolean checkBlockList(String packageName) {
        if (TextUtils.equals("com.android.systemui", packageName)) {
            return true;
        }
        try {
            //检查是否为launcher应用
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> resolveInfos;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                resolveInfos = PackageManagerUtils.queryIntentActivities(homeIntent, null, PackageManager.MATCH_ALL, 0);
            } else {
                resolveInfos = PackageManagerUtils.queryIntentActivities(homeIntent, null, 0, 0);
            }
            Logger.d(TAG, "launcher apps:" + resolveInfos);
            if (resolveInfos != null) {
                for (ResolveInfo resolveInfo : resolveInfos) {
                    if (!TextUtils.equals("com.android.settings", packageName) && TextUtils.equals(resolveInfo.activityInfo.packageName, packageName)) {
                        return true;
                    }
                }
            }

            //检查是否为键盘应用
            Intent keyboardIntent = new Intent("android.view.InputMethod");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                resolveInfos = PackageManagerUtils.queryIntentServices(keyboardIntent, null, PackageManager.MATCH_ALL, 0);
            } else {
                resolveInfos = PackageManagerUtils.queryIntentServices(keyboardIntent, null, 0, 0);
            }
            Logger.d(TAG, "keyboard apps:" + resolveInfos);
            if (resolveInfos != null) {
                for (ResolveInfo resolveInfo : resolveInfos) {
                    if (TextUtils.equals(resolveInfo.serviceInfo.packageName, packageName)) {
                        return true;
                    }
                }
            }

            //检查是否为无界面应用
            PackageInfo packageInfo = PackageManagerUtils.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES, 0);
            if (packageInfo != null && packageInfo.activities != null && packageInfo.activities.length == 0) {
                Logger.d(TAG, "no user interface app:" + resolveInfos);
                return true;
            }
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
