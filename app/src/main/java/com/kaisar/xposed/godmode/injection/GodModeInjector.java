package com.kaisar.xposed.godmode.injection;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.bridge.ManagerObserver;
import com.kaisar.xposed.godmode.injection.hook.ActivityLifecycleHook;
import com.kaisar.xposed.godmode.injection.hook.DispatchKeyEventHook;
import com.kaisar.xposed.godmode.injection.hook.DisplayPropertiesHook;
import com.kaisar.xposed.godmode.injection.hook.EventHandlerHook;
import com.kaisar.xposed.godmode.injection.hook.SystemPropertiesHook;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.PackageManagerUtils;
import com.kaisar.xposed.godmode.injection.util.Property;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.service.GodModeManagerService;
import com.kaisar.xposed.godmode.util.XposedEnvironment;
import com.kaisar.xservicemanager.XServiceManager;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;
/**
 * Created by jrsen on 17-10-13.
 */

public final class GodModeInjector implements IXposedHookLoadPackage {

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

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!loadPackageParam.isFirstApplication) {
            return;
        }
        GodModeInjector.loadPackageParam = loadPackageParam;
        final String packageName = loadPackageParam.packageName;
        switch (packageName) {
            case "android"://Run in system process
                Logger.d(TAG, "inject GodModeManagerService as system service.");
                XServiceManager.initForSystemServer();
                XServiceManager.registerService("godmode", new XServiceManager.ServiceFetcher<Binder>() {
                    @Override
                    public Binder createService(Context ctx) {
                        return new GodModeManagerService(ctx);
                    }
                });
                return;
            case BuildConfig.APPLICATION_ID://Run in God's management process
                XposedHelpers.findAndHookMethod(XposedEnvironment.class.getName(), loadPackageParam.classLoader, "isModuleActive", Context.class, XC_MethodReplacement.returnConstant(true));
                return;
            default://Run in other application processes
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
            resolveInfos = PackageManagerUtils.queryIntentActivities(homeIntent, null, PackageManager.MATCH_ALL, 0);
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
            resolveInfos = PackageManagerUtils.queryIntentServices(keyboardIntent, null, PackageManager.MATCH_ALL, 0);
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
        // TODO: 不能hook ViewGroup FrameLayout LinearLayout 不然会无法显示控制
        XposedHelpers.findAndHookMethod(View.class.getName(), loadPackageParam.classLoader, "onDraw", Canvas.class, displayPropertiesHook);
        XposedHelpers.findAndHookMethod(TextView.class.getName(), loadPackageParam.classLoader, "onDraw", Canvas.class, displayPropertiesHook);
        XposedHelpers.findAndHookMethod(ImageView.class.getName(), loadPackageParam.classLoader, "onDraw", Canvas.class, displayPropertiesHook);

//        //hook debug layout
//        if (Build.VERSION.SDK_INT < 29) {
//            SystemPropertiesHook systemPropertiesHook = new SystemPropertiesHook();
//            switchProp.addOnPropertyChangeListener(systemPropertiesHook);
//            XposedHelpers.findAndHookMethod("android.os.SystemProperties", ClassLoader.getSystemClassLoader(), "native_get_boolean", String.class, boolean.class, systemPropertiesHook);
//        } else {
//            DisplayPropertiesHook displayPropertiesHook = new DisplayPropertiesHook();
//            switchProp.addOnPropertyChangeListener(displayPropertiesHook);
//            XposedHelpers.findAndHookMethod("android.sysprop.DisplayProperties", ClassLoader.getSystemClassLoader(), "debug_layout", displayPropertiesHook);
//        }
//
//        //Disable show layout margin bound
//        XposedHelpers.findAndHookMethod(ViewGroup.class, "onDebugDrawMargins", Canvas.class, Paint.class, XC_MethodReplacement.DO_NOTHING);
//
//        //Disable GM component show layout bounds
//        XC_MethodHook disableDebugDraw = new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                View view = (View) param.thisObject;
//                if (ViewHelper.TAG_GM_CMP.equals(view.getTag())) {
//                    param.setResult(null);
//                }
//            }
//        };
//        XposedHelpers.findAndHookMethod(ViewGroup.class, "onDebugDraw", Canvas.class, disableDebugDraw);
//        XposedHelpers.findAndHookMethod(View.class, "debugDrawFocus", Canvas.class, disableDebugDraw);

        EventHandlerHook eventHandlerHook = new EventHandlerHook();
        switchProp.addOnPropertyChangeListener(eventHandlerHook);
        //Volume key select
        //XposedHelpers.findAndHookMethod(Activity.class, "dispatchKeyEvent", KeyEvent.class, eventHandlerHook);
        //Drag view support
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent", MotionEvent.class, eventHandlerHook);
    }
}
