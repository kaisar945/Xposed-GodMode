package com.kaisar.xposed.godmode;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

/**
 * Created by jrsen on 17-10-16.
 */

public final class GodModeApplication extends Application {

    public static final String TAG = "GodMode";

    @Override
    protected void attachBaseContext(Context base) {
        CrashHandler.install(base);
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startQuickSettingServiceIfEnabled();
    }

    private void startQuickSettingServiceIfEnabled() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enable = sp.getBoolean("quick_setting", false);
        if (enable) {
            Intent service = new Intent(this, QuickSettingsCompatService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        }
    }

}
