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
    private static File sLogFile;

    private final Context context;

    static void install(Context context) {
        sLogFile = new File(context.getFilesDir(), BUG_REPORT_FILE);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    CrashHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        recordCrashLog(e);
        Logger.e(TAG, "Crash", e);
        restart(context);
    }

    private void recordCrashLog(Throwable t) {
        try {
            try (FileChannel fileChannel = new FileOutputStream(sLogFile).getChannel()) {
                String stackTraceString = Logger.getStackTraceString(t);
                fileChannel.write(ByteBuffer.wrap(stackTraceString.getBytes()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadCrashLog() {
        if (sLogFile.exists()) {
            try {
                try (FileChannel fileChannel = new FileInputStream(sLogFile).getChannel()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
                    fileChannel.read(byteBuffer);
                    byteBuffer.flip();
                    return new String(byteBuffer.array());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static void clearCrashLog() {
        //noinspection ResultOfMethodCallIgnored
        sLogFile.delete();
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
