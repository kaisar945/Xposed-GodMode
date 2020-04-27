package com.viewblocker.jrsen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.viewblocker.jrsen.service.InjectBridgeService;

/**
 * Created by jrsen on 18-1-26.
 */

public final class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        InjectBridgeService bridgeService = InjectBridgeService.getBridge(context);
        if (bridgeService.isInEditMode()) {
            bridgeService.setEditModeEnable(false);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean("editor_switch", false).apply();
        }
    }

}
