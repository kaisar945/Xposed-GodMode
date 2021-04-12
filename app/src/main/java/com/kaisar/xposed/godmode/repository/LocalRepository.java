package com.kaisar.xposed.godmode.repository;

import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;

public final class LocalRepository {

    private static AppRules sAppRules;

    public static AppRules getAllAppRules() {
        if (sAppRules == null) {
            sAppRules = GodModeManager.getDefault().getAllRules();
        }
        return sAppRules;
    }

    public static ActRules getAppRule(String packageName) {
        return getAllAppRules().get(packageName);
    }
}
