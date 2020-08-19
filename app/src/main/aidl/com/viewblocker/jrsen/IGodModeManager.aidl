package com.viewblocker.jrsen;

import com.viewblocker.jrsen.IObserver;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.analytics.CrashRecord;
import android.graphics.Bitmap;

interface IGodModeManager {

    void setEditMode(boolean enable);

    boolean isInEditMode();

    void addObserver(String packageName, in IObserver observer);

    Map getAllRules();

    ActRules getRules(String packageName);

    boolean writeRule(String packageName, in ViewRule viewRule, in Bitmap bitmap);

    boolean updateRule(String packageName, in ViewRule viewRule);

    boolean deleteRule(String packageName, in ViewRule viewRule);

    boolean deleteRules(String packageName);

}
