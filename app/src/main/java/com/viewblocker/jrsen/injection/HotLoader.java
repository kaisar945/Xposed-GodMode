package com.viewblocker.jrsen.injection;

import android.content.pm.PackageInfo;
import android.os.Process;
import android.util.Log;

import com.viewblocker.jrsen.BuildConfig;
import com.viewblocker.jrsen.injection.util.PackageManagerUtils;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 开发更新模块无需重启 需要在xposed_init指定
 */
public final class HotLoader implements IXposedHookLoadPackage {

    private static final String TAG = HotLoader.class.getSimpleName();
    private static final Class<? extends IXposedHookLoadPackage> XPOSED_INIT_CLASS = BlockerInjector.class;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            if (Process.myUid() == Process.SYSTEM_UID) {
                IXposedHookLoadPackage iXposedHookLoadPackage = XPOSED_INIT_CLASS.newInstance();
                iXposedHookLoadPackage.handleLoadPackage(lpparam);
            } else {
                PackageInfo packageInfo = PackageManagerUtils.getPackageInfo(BuildConfig.APPLICATION_ID, 0, 0);
                Log.i(TAG, "hot load dex path:" + packageInfo.applicationInfo.sourceDir + " uid=" + Process.myUid());
                PathClassLoader classLoader = new PathClassLoader(packageInfo.applicationInfo.sourceDir, IXposedHookLoadPackage.class. getClassLoader());
                IXposedHookLoadPackage iXposedHookLoadPackage = (IXposedHookLoadPackage) classLoader.loadClass(XPOSED_INIT_CLASS.getName()).newInstance();
                iXposedHookLoadPackage.handleLoadPackage(lpparam);
            }
        } catch (Throwable t) {
            Log.e(TAG, "loader exception", t);
        }
    }

}
