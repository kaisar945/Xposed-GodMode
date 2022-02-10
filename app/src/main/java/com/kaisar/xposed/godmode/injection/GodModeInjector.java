package com.kaisar.xposed.godmode.injection;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.bridge.ManagerObserver;
import com.kaisar.xposed.godmode.injection.hook.ActivityLifecycleHook;
import com.kaisar.xposed.godmode.injection.hook.DispatchKeyEventHook;
import com.kaisar.xposed.godmode.injection.hook.DisplayPropertiesHook;
import com.kaisar.xposed.godmode.injection.hook.EventHandlerHook;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.PackageManagerUtils;
import com.kaisar.xposed.godmode.injection.util.Property;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.service.GodModeManagerService;
import com.kaisar.xservicemanager.XServiceManager;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by jrsen on 17-10-13.
 */

public final class GodModeInjector implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public final static Property<Boolean> switchProp = new Property<>();
    public final static Property<ActRules> actRuleProp = new Property<>();
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;
    private static State state = State.UNKNOWN;
    private static DispatchKeyEventHook dispatchKeyEventHook = new DispatchKeyEventHook();

    enum State {
        UNKNOWN,
        ALLOWED,
        BLOCKED
    }

    public static void notifyEditModeChanged(boolean enable) {
        if (state == State.UNKNOWN) {
            state = checkBlockList(loadPackageParam.packageName) ? State.BLOCKED : State.ALLOWED;
        }
        if (state == State.ALLOWED) {
            switchProp.set(enable);
        }
        dispatchKeyEventHook.setdisplay(enable);
    }

    public static void notifyViewRulesChanged(ActRules actRules) {
        actRuleProp.set(actRules);
    }

    private static String modulePath;
    public static Resources moduleRes;

    // Injector Res
    @Override
    public void initZygote(StartupParam startupParam) {
        modulePath = startupParam.modulePath;
        moduleRes = XModuleResources.createInstance(modulePath, null);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!loadPackageParam.isFirstApplication) {
            return;
        }
        GodModeInjector.loadPackageParam = loadPackageParam;
        final String packageName = loadPackageParam.packageName;
        if ("android".equals(packageName)) {//Run in system process
            Logger.d(TAG, "inject GodModeManagerService as system service.");
            XServiceManager.initForSystemServer();
            XServiceManager.registerService("godmode", (XServiceManager.ServiceFetcher<Binder>) GodModeManagerService::new);
        } else {//Run in other application processes
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //Volume key select old
                    dispatchKeyEventHook.setactivity((Activity) param.thisObject);
                    super.afterHookedMethod(param);
                }
            });
            registerHook();
            GodModeManager gmManager = GodModeManager.getDefault();
            gmManager.addObserver(loadPackageParam.packageName, new ManagerObserver());
            switchProp.set(gmManager.isInEditMode());
            actRuleProp.set(gmManager.getRules(loadPackageParam.packageName));
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

    private void registerHook() {
        //hook activity#lifecycle block view
        ActivityLifecycleHook lifecycleHook = new ActivityLifecycleHook();
        actRuleProp.addOnPropertyChangeListener(lifecycleHook);
        XposedHelpers.findAndHookMethod(Activity.class, "onPostResume", lifecycleHook);
        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", lifecycleHook);

        DisplayPropertiesHook displayPropertiesHook = new DisplayPropertiesHook();
        switchProp.addOnPropertyChangeListener(displayPropertiesHook);
        XposedHelpers.findAndHookConstructor(View.class, Context.class, displayPropertiesHook);

        EventHandlerHook eventHandlerHook = new EventHandlerHook();
        switchProp.addOnPropertyChangeListener(eventHandlerHook);
        //Volume key select
        //XposedHelpers.findAndHookMethod(Activity.class, "dispatchKeyEvent", KeyEvent.class, eventHandlerHook);
        //Drag view support
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent", MotionEvent.class, eventHandlerHook);
    }

}
