package com.kaisar.xposed.godmode;

import android.app.Application;
import android.content.Context;

/**
 * Created by jrsen on 17-10-16.
 */

public final class GodModeApplication extends Application {

    public static final String TAG = "GodMode";
    private static GodModeApplication sApplication;

    public GodModeApplication() {
        sApplication = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        CrashHandler.install(base);
        super.attachBaseContext(base);
    }

    public static GodModeApplication getApplication() {
        return sApplication;
    }

}
