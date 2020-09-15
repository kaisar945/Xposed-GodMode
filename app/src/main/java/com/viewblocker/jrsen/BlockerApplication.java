package com.viewblocker.jrsen;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * Created by jrsen on 17-10-16.
 */

public final class BlockerApplication extends Application {

    public static final String TAG = "ViewBlocker";

    @Override
    public void onCreate() {
        CrashHandler.init(this);
        super.onCreate();
        initQuickSettingService();
    }

    private void initQuickSettingService() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean quickSettingEnable = sp.getBoolean("quick_setting", false);
        if (quickSettingEnable) {
            QuickSettingsCompatService.setComponentState(this, true);
            Intent service = new Intent(this, QuickSettingsCompatService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        }
    }

}
