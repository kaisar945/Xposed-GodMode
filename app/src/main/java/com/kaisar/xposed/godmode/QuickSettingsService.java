package com.kaisar.xposed.godmode;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.text.TextUtils;

import com.kaisar.xposed.godmode.fragment.GeneralPreferenceFragment;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;

/**
 * Created by jrsen on 17-10-26.
 */

@TargetApi(Build.VERSION_CODES.N)
public final class QuickSettingsService extends TileService implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onStartListening() {
        super.onStartListening();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onClick() {
        setEditModeEnable(!isEditMode());
        updateTile();
    }

    private void updateTile() {
        Tile tile = this.getQsTile();
        if (tile != null) {
            boolean isActive = isEditMode();

//         Change the tile to match the service status.
            Icon newIcon = Icon.createWithResource(this, isActive ? R.drawable.ic_angel_normal : R.drawable.ic_angel_disable);
            int newState = isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;

            // Change the UI of the tile.
            tile.setIcon(newIcon);
            tile.setState(newState);

            // Need to call updateTile for the tile to pick up changes.
            tile.updateTile();
        }
    }

    private void setEditModeEnable(boolean enable) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putBoolean("editor_switch", enable).apply();
        GodModeManager.getDefault().setEditMode(enable);
    }

    public boolean isEditMode() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean("editor_switch", false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(GeneralPreferenceFragment.KEY_EDITOR_SWITCH, key)) {
            updateTile();
        }
    }
}
