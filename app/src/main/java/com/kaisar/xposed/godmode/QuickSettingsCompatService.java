package com.kaisar.xposed.godmode;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.kaisar.xposed.godmode.fragment.GeneralPreferenceFragment;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;

/**
 * Created by jrsen on 17-10-26.
 */

public final class QuickSettingsCompatService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "QuickSettingsCompatService";
    private static final String ACTION_CLOSE_NOTIFICATION = "action_close_notification";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        startForeground(1, buildNotification());
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (TextUtils.equals(intent.getAction(), ACTION_CLOSE_NOTIFICATION)) {
                stopForeground(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    deleteNotificationChannel();
                }
                stopSelf();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                sp.edit().putBoolean(GeneralPreferenceFragment.KEY_QUICK_SETTING, false).apply();
                return START_NOT_STICKY;
            }
            setEditModeEnable(!isEditMode());
            startForeground(1, buildNotification());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(TAG, "GodChannel", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void deleteNotificationChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.deleteNotificationChannel(TAG);
    }

    private Notification buildNotification() {
        Intent exitIntent = new Intent(this, QuickSettingsCompatService.class);
        exitIntent.setAction(ACTION_CLOSE_NOTIFICATION);
        PendingIntent exitPendingIntent = PendingIntent.getService(this, 0, exitIntent, 0);

        Intent openIntent = new Intent(this, SettingsActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, 0);

        Intent intent = new Intent(this, QuickSettingsCompatService.class);
        intent.setAction(Intent.ACTION_EDIT);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, TAG)
                .setSmallIcon(R.drawable.ic_angel_small)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_angel_normal))
                .setContentTitle(getText(R.string.app_name))
                .setContentText(isEditMode() ? getString(R.string.enter_edit) : getString(R.string.exit_edit))
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.close), exitPendingIntent)
                .addAction(android.R.drawable.ic_menu_manage, getString(R.string.manage), openPendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

    public static void setComponentState(Context context, boolean enable) {
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, QuickSettingsCompatService.class);
        pm.setComponentEnabledSetting(componentName, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(GeneralPreferenceFragment.KEY_EDITOR_SWITCH, key)) {
            startForeground(1, buildNotification());
        }
    }
}
