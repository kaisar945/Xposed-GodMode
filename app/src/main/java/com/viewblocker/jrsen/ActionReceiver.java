package com.viewblocker.jrsen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.viewblocker.jrsen.injection.bridge.GodModeManager;

/**
 * Created by jrsen on 18-1-26.
 */

public final class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GodModeManager manager = GodModeManager.getDefault();
        if (manager.isInEditMode()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean("editor_switch", false).apply();
            manager.setEditMode(false);
        }
    }

}
