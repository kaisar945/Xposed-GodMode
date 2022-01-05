package com.kaisar.xposed.godmode;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.kaisar.xposed.godmode.injection.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by jrsen on 17-10-21.
 */

public final class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String LOG_FILE = "crash_info.log";

    private final Context mContext;

    static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    public static String getLastCrashInfo(Context context) {
        File logFile = new File(context.getExternalCacheDir(), LOG_FILE);
        if (logFile.exists()) {
            try {
                try (FileChannel fileChannel = new FileInputStream(logFile).getChannel()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
                    fileChannel.read(byteBuffer);
                    byteBuffer.flip();
                    return new String(byteBuffer.array());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //noinspection ResultOfMethodCallIgnored
                logFile.delete();
            }
        }
        return null;
    }

    CrashHandler(Context context) {
        mContext = context;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        recordCrash(t, e);
        restartSelf();
    }

    private void recordCrash(Thread t, Throwable e) {
        try {
            File logFile = new File(mContext.getExternalCacheDir(), LOG_FILE);
            try (FileChannel fileChannel = new FileOutputStream(logFile).getChannel()) {
                String stackTraceString = Logger.getStackTraceString(e);
                fileChannel.write(ByteBuffer.wrap(stackTraceString.getBytes()));
            }
        } catch (IOException ignore) {
//            ignore.printStackTrace();
        }
        Logger.e(TAG, String.format("Crash on %s thread", t.getName()), e);
    }


    private void restartSelf() {
        Intent intent = new Intent(mContext, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent restartIntent = PendingIntent.getActivity(mContext, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 5, restartIntent);
        }
        //结束进程
        System.exit(1);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
