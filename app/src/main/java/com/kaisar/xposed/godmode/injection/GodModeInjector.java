package com.kaisar.xposed.godmode.injection;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.bridge.ManagerObserver;
import com.kaisar.xposed.godmode.injection.hook.ActivityLifecycleHook;
import com.kaisar.xposed.godmode.injection.hook.DispatchKeyEventHook;
import com.kaisar.xposed.godmode.injection.hook.DisplayPropertiesHook;
import com.kaisar.xposed.godmode.injection.hook.EventHandlerHook;
import com.kaisar.xposed.godmode.injection.hook.SystemPropertiesHook;
import com.kaisar.xposed.godmode.injection.hook.SystemPropertiesStringHook;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.PackageManagerUtils;
import com.kaisar.xposed.godmode.injection.util.Property;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.service.GodModeManagerService;
import com.kaisar.xservicemanager.XServiceManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
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
        if (R.string.res_inject_success >>> 24 == 0x7f) {
            XposedBridge.log("package id must NOT be 0x7f, reject loading...");
            return;
        }
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
                    Activity activity = (Activity) param.thisObject;
                    dispatchKeyEventHook.setactivity(activity);
                    injectModuleResources(activity.getResources());
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

    /**
     * Inject resources into hook software - Code from qnotified
     * @param res Inject software resources
     */
    public static void injectModuleResources(Resources res) {
        if (res == null) {
            return;
        }
        try {
            res.getString(R.string.res_inject_success);
            return;
        } catch (Resources.NotFoundException ignored) {
        }
        try {
            String sModulePath = modulePath;
            if (sModulePath == null) {
                throw new RuntimeException(
                        "get module path failed, loader=" + GodModeInjector.class.getClassLoader());
            }
            AssetManager assets = res.getAssets();
            @SuppressLint("DiscouragedPrivateApi")
            Method addAssetPath = AssetManager.class
                    .getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            int cookie = (int) addAssetPath.invoke(assets, sModulePath);
            try {
                Logger.i(TAG, "injectModuleResources: " + res.getString(R.string.res_inject_success));
            } catch (Resources.NotFoundException e) {
                Logger.e(TAG, "Fatal: injectModuleResources: test injection failure!");
                Logger.e(TAG, "injectModuleResources: cookie=" + cookie + ", path=" + sModulePath
                        + ", loader=" + GodModeInjector.class.getClassLoader());
                long length = -1;
                boolean read = false;
                boolean exist = false;
                boolean isDir = false;
                try {
                    File f = new File(sModulePath);
                    exist = f.exists();
                    isDir = f.isDirectory();
                    length = f.length();
                    read = f.canRead();
                } catch (Throwable e2) {
                    Logger.e(TAG, "Open module error", e2);
                }
                Logger.e(TAG, "sModulePath: exists = " + exist + ", isDirectory = " + isDir + ", canRead = "
                        + read + ", fileLength = " + length);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Inject module resources error", e);
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

//        DisplayPropertiesHook displayPropertiesHook = new DisplayPropertiesHook();
//        switchProp.addOnPropertyChangeListener(displayPropertiesHook);
//        XposedHelpers.findAndHookConstructor(View.class, Context.class, displayPropertiesHook);

        // Hook debug layout
        try {
            if (Build.VERSION.SDK_INT < 29) {
                SystemPropertiesHook systemPropertiesHook = new SystemPropertiesHook();
                switchProp.addOnPropertyChangeListener(systemPropertiesHook);
                XposedHelpers.findAndHookMethod("android.os.SystemProperties", ClassLoader.getSystemClassLoader(), "native_get_boolean", String.class, boolean.class, systemPropertiesHook);
            } else {
                SystemPropertiesStringHook systemPropertiesStringHook = new SystemPropertiesStringHook();
                switchProp.addOnPropertyChangeListener(systemPropertiesStringHook);
                XposedBridge.hookAllMethods(XposedHelpers.findClass("android.os.SystemProperties", ClassLoader.getSystemClassLoader()), "native_get", systemPropertiesStringHook);

                DisplayPropertiesHook displayPropertiesHook = new DisplayPropertiesHook();
                switchProp.addOnPropertyChangeListener(displayPropertiesHook);
                XposedHelpers.findAndHookMethod("android.sysprop.DisplayProperties", ClassLoader.getSystemClassLoader(), "debug_layout", displayPropertiesHook);
            }

            //Disable show layout margin bound
            XposedHelpers.findAndHookMethod(ViewGroup.class, "onDebugDrawMargins", Canvas.class, Paint.class, XC_MethodReplacement.DO_NOTHING);

            //Disable GM component show layout bounds
            XC_MethodHook disableDebugDraw = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    View view = (View) param.thisObject;
                    if (ViewHelper.TAG_GM_CMP.equals(view.getTag())) {
                        param.setResult(null);
                    }
                }
            };
            XposedHelpers.findAndHookMethod(ViewGroup.class, "onDebugDraw", Canvas.class, disableDebugDraw);
            XposedHelpers.findAndHookMethod(View.class, "debugDrawFocus", Canvas.class, disableDebugDraw);
        } catch (Throwable e) {
            Logger.e(TAG, "Hook debug layout error", e);
        }


        EventHandlerHook eventHandlerHook = new EventHandlerHook();
        switchProp.addOnPropertyChangeListener(eventHandlerHook);
        //Volume key select
        //XposedHelpers.findAndHookMethod(Activity.class, "dispatchKeyEvent", KeyEvent.class, eventHandlerHook);
        //Drag view support
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent", MotionEvent.class, eventHandlerHook);
    }

}
