package com.kaisar.xposed.godmode;

import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.ViewRule;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

interface IGodModeManager {

    void setEditMode(boolean enable);

    boolean isInEditMode();

    void addObserver(String packageName, in IObserver observer);

    AppRules getAllRules();

    ActRules getRules(String packageName);

    boolean writeRule(String packageName, in ViewRule viewRule, in Bitmap bitmap);

    boolean updateRule(String packageName, in ViewRule viewRule);

    boolean deleteRule(String packageName, in ViewRule viewRule);

    boolean deleteRules(String packageName);

    ParcelFileDescriptor openFile(String filePath, int mode);

}
