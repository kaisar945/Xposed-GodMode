package com.viewblocker.jrsen.injection;

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.kaisar.xservicemanager.XServiceManager;
import com.viewblocker.jrsen.BuildConfig;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.injection.bridge.ManagerObserver;
import com.viewblocker.jrsen.injection.hook.ActivityLifecycleHook;
import com.viewblocker.jrsen.injection.hook.DispatchKeyEventHook;
import com.viewblocker.jrsen.injection.hook.DispatchTouchEventHook;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.injection.util.PackageManagerUtils;
import com.viewblocker.jrsen.injection.util.Property;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.service.GodModeManagerService;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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
            case "android":
                // 注册系统服务
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
                //检测上帝模式模块是否开启
                XposedHelpers.findAndHookMethod("com.viewblocker.jrsen.util.XposedEnvironment", loadPackageParam.classLoader, "isModuleActive", Context.class, XC_MethodReplacement.returnConstant(true));
                break;
            default:
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
        switchProp.addOnPropertyChangeListener(new OnDebugLayoutChangeListener());

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
        BlockerInjector.switchProp.addOnPropertyChangeListener(dispatchKeyEventHook);

        //Drag view support
        XposedHelpers.findAndHookMethod(View.class, "dispatchTouchEvent", MotionEvent.class, new DispatchTouchEventHook());
    }

    static final class OnDebugLayoutChangeListener implements Property.OnPropertyChangeListener<Boolean> {

        @Override
        public void onPropertyChange(Boolean layout) {
            Logger.d(TAG, String.format("debug layout enable %b", layout));
            try {
                @SuppressLint("PrivateApi") Object windowManagerGlobal = XposedHelpers.callStaticMethod(Class.forName("android.view.WindowManagerGlobal"), "getInstance");
                @SuppressWarnings("rawtypes") ArrayList roots = (ArrayList) XposedHelpers.getObjectField(windowManagerGlobal, "mRoots");
                @SuppressLint("PrivateApi") final int MSG_INVALIDATE_WORLD = XposedHelpers.getStaticIntField(Class.forName("android.view.ViewRootImpl"), "MSG_INVALIDATE_WORLD");
                for (Object viewRootImpl : roots) {
                    Object attachInfo = XposedHelpers.getObjectField(viewRootImpl, "mAttachInfo");
                    boolean debugLayout = XposedHelpers.getBooleanField(attachInfo, "mDebugLayout");
                    if (layout != debugLayout) {
                        XposedHelpers.setBooleanField(attachInfo, "mDebugLayout", layout);
                        Handler handler = (Handler) XposedHelpers.getObjectField(viewRootImpl, "mHandler");
                        if (!handler.hasMessages(MSG_INVALIDATE_WORLD)) {
                            handler.sendEmptyMessageDelayed(MSG_INVALIDATE_WORLD, 200);
                        }
                    }
                }
            } catch (Throwable tr) {
                Logger.e(TAG, "debug layout exception", tr);
            }
        }
    }


}
