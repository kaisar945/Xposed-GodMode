package com.kaisar.xposed.godmode.injection.bridge;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.injection.GodModeInjector;
import com.kaisar.xposed.godmode.rule.ActRules;


/**
 * Created by jrsen on 17-10-18.
 */

public final class ManagerObserver extends IObserver.Stub implements Handler.Callback {

    private final Handler mHandler = new Handler(Looper.getMainLooper(), this);
    private static final int ACTION_EDIT_MODE_CHANGED = 0;
    private static final int ACTION_VIEW_RULES_CHANGED = 1;

    @Override
    public void onEditModeChanged(boolean enable) {
        mHandler.obtainMessage(ACTION_EDIT_MODE_CHANGED, enable).sendToTarget();
    }

    @Override
    public void onViewRuleChanged(String packageName, ActRules actRules) {
        mHandler.obtainMessage(ACTION_VIEW_RULES_CHANGED, actRules).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == ACTION_EDIT_MODE_CHANGED) {
            GodModeInjector.notifyEditModeChanged((Boolean) msg.obj);
        } else if (msg.what == ACTION_VIEW_RULES_CHANGED) {
            GodModeInjector.notifyViewRulesChanged((ActRules) msg.obj);
        }
        return true;
    }

}

