package com.kaisar.xposed.godmode.injection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Binder;
import android.os.Build;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.kaisar.xservicemanager.XServiceManager;
import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.bridge.ManagerObserver;
import com.kaisar.xposed.godmode.injection.hook.ActivityLifecycleHook;
import com.kaisar.xposed.godmode.injection.hook.DispatchKeyEventHook;
import com.kaisar.xposed.godmode.injection.hook.DispatchTouchEventHook;
import com.kaisar.xposed.godmode.injection.hook.DisplayPropertiesHook;
import com.kaisar.xposed.godmode.injection.hook.SystemPropertiesHook;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.PackageManagerUtils;
import com.kaisar.xposed.godmode.injection.util.Property;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.service.GodModeManagerService;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;


/**
 * Created by jrsen on 17-10-13.
 */

public final class GodModeInjector implements IXposedHookLoadPackage {

    public final static Property<Boolean> switchProp = new Property<>();
    public final static Property<ActRules> actRuleProp = new Property<>();
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;

    public static void notifyEditModeChanged(boolean enable) {
        if (!checkBlockList(loadPackageParam.packageName)) {
            switchProp.set(enable);
        }
    }

    public static void notifyViewRulesChanged(ActRules actRules) {
        actRuleProp.set(actRules);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!loadPackageParam.isFirstApplication) {
            return;
        }
        GodModeInjector.loadPackageParam = loadPackageParam;
        final String packageName = loadPackageParam.packageName;
        switch (packageName) {
            case "android":
                // running in system_server process for register GodMode service
                Logger.i(TAG, "inject GodModeManagerService as system service.");
                XServiceManager.initForSystemServer();
                XServiceManager.registerService("godmode", new XServiceManager.ServiceFetcher<Binder>() {
                    @Override
                    public Binder createService(Context ctx) {
                        return new GodModeManagerService(ctx);
                    }
                });
                break;
            case BuildConfig.APPLICATION_ID:
                // running in manager process for check module active
                XposedHelpers.findAndHookMethod("com.viewblocker.jrsen.util.XposedEnvironment", loadPackageParam.classLoader, "isModuleActive", Context.class, XC_MethodReplacement.returnConstant(true));
                break;
            default:
                // running in other app process for enable godmode
                if (checkBlockList(loadPackageParam.packageName)) {
                    Logger.i(TAG, String.format("%s in block list.", loadPackageParam.packageName));
                    return;
                }
                initHook();
                GodModeManager manager = GodModeManager.getDefault();
                manager.addObserver(loadPackageParam.packageName, new ManagerObserver());
                switchProp.set(manager.isInEditMode());
                actRuleProp.set(manager.getRules(loadPackageParam.packageName));
                break;
        }
    }

    private static boolean checkBlockList(String packageName) {
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
//            Logger.d(TAG, "launcher apps:" + resolveInfos);
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
//            Logger.d(TAG, "keyboard apps:" + resolveInfos);
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
//                Logger.d(TAG, "no user interface app:" + resolveInfos);
                return true;
            }
        } catch (Throwable t) {
            Logger.e(TAG, "checkWhiteListPackage crash", t);
        }
        return false;
    }

    private void initHook() {
        //hook activity#lifecycle block view
        ActivityLifecycleHook lifecycleHook = new ActivityLifecycleHook();
        actRuleProp.addOnPropertyChangeListener(lifecycleHook);
        XposedHelpers.findAndHookMethod(Activity.class, "onPostResume", lifecycleHook);
        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", lifecycleHook);

        //hook debug layout
        if (Build.VERSION.SDK_INT < 29) {
            SystemPropertiesHook systemPropertiesHook = new SystemPropertiesHook();
            switchProp.addOnPropertyChangeListener(systemPropertiesHook);
            XposedHelpers.findAndHookMethod("android.os.SystemProperties", ClassLoader.getSystemClassLoader(), "getBoolean", String.class, boolean.class, systemPropertiesHook);
        } else {
            DisplayPropertiesHook displayPropertiesHook = new DisplayPropertiesHook();
            switchProp.addOnPropertyChangeListener(displayPropertiesHook);
            XposedHelpers.findAndHookMethod("android.sysprop.DisplayProperties", ClassLoader.getSystemClassLoader(), "debug_layout", displayPropertiesHook);
        }

        //Disable show layout margin bound
        XposedHelpers.findAndHookMethod(ViewGroup.class, "onDebugDrawMargins", Canvas.class, Paint.class, XC_MethodReplacement.DO_NOTHING);

        //Disable GM component show layout bounds
        XC_MethodHook disableDebugDraw = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                if (ViewHelper.TAG_GM_CMP.equals(view.getTag())) {
                    param.setResult(null);
                }
            }
        };
        XposedHelpers.findAndHookMethod(ViewGroup.class, "onDebugDraw", Canvas.class, disableDebugDraw);
        XposedHelpers.findAndHookMethod(View.class, "debugDrawFocus", Canvas.class, disableDebugDraw);

        //Volume key select
        DispatchKeyEventHook dispatchKeyEventHook = new DispatchKeyEventHook();
        XposedHelpers.findAndHookMethod(Activity.class, "dispatchKeyEvent", KeyEvent.class, dispatchKeyEventHook);
        GodModeInjector.switchProp.addOnPropertyChangeListener(dispatchKeyEventHook);

        //Drag view support
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent", MotionEvent.class, new DispatchTouchEventHook());
    }

}
