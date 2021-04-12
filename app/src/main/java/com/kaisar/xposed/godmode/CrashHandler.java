package com.kaisar.xposed.godmode;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.kaisar.xposed.godmode.injection.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

/**
 * Created by jrsen on 17-10-21.
 */

public final class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String BUG_REPORT_FILE = "crash_log.txt";

    private final Context context;

    static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    CrashHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        recordCrash(e);
        Logger.e(TAG, "Crash", e);
        restart(context);
    }

    private static File getLogFile(Context context) {
        return new File(context.getCacheDir(), BUG_REPORT_FILE);
    }

    private void recordCrash(Throwable t) {
        try {
            File logFile = getLogFile(context);
            try (FileChannel fileChannel = new FileOutputStream(logFile).getChannel()) {
                String stackTraceString = Logger.getStackTraceString(t);
                fileChannel.write(ByteBuffer.wrap(stackTraceString.getBytes()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String detectCrash(Context context) {
        File logFile = getLogFile(context);
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

    public static void restart(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent restartIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 5, restartIntent);
        }
        //结束进程
        System.exit(1);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
