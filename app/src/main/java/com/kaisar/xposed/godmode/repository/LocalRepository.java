package com.kaisar.xposed.godmode.repository;

import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.rule.AppRules;

public final class LocalRepository {

    public static void addObserver(String packageName, IObserver observer) {
        GodModeManager.getDefault().addObserver(packageName, observer);
    }

    public static AppRules loadAppRules() {
        return GodModeManager.getDefault().getAllRules();
    }

}
