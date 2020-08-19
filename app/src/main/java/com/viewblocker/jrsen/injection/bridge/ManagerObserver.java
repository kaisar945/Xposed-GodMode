package com.viewblocker.jrsen.injection.bridge;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import com.viewblocker.jrsen.IObserver;
import com.viewblocker.jrsen.injection.BlockerInjector;
import com.viewblocker.jrsen.rule.ActRules;


/**
 * Created by jrsen on 17-10-18.
 */

public final class ManagerObserver extends IObserver.Stub implements Handler.Callback {

    private final Handler handler = new Handler(Looper.getMainLooper(), this);
    private static final int ACTION_EDIT_MODE_STATE_CHANGED = 0;
    private static final int ACTION_VIEW_RULES_CHANGED = 1;

    @Override
    public void onEditModeChanged(boolean enable) throws RemoteException {
        handler.obtainMessage(ACTION_EDIT_MODE_STATE_CHANGED, enable).sendToTarget();
    }

    @Override
    public void onViewRuleChanged(ActRules actRules) throws RemoteException {
        handler.obtainMessage(ACTION_VIEW_RULES_CHANGED, actRules).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == ACTION_EDIT_MODE_STATE_CHANGED) {
            BlockerInjector.switchProp.set((Boolean) msg.obj);
        } else if (msg.what == ACTION_VIEW_RULES_CHANGED) {
            BlockerInjector.actRuleProp.set((ActRules) msg.obj);
        }
        return true;
    }

}

