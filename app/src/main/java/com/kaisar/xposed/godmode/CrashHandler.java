package com.kaisar.xposed.godmode;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.kaisar.xposed.godmode.injection.util.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by jrsen on 17-10-21.
 */

final class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static final String BUG_REPORT_FILE = "crash_log.txt";
    private static File logFile;

    private final Context context;

    static void init(Context context) {
        logFile = new File(context.getFilesDir(), BUG_REPORT_FILE);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    CrashHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        saveCrashLog(e);
        Logger.e(TAG, "Crash", e);
        restart(context);
    }

    private void saveCrashLog(Throwable t) {
        try (FileWriter fw = new FileWriter(logFile)) {
            Properties properties = new Properties();
            properties.setProperty("stack_trace", Logger.getStackTraceString(t));
            properties.store(fw, "godmode");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadCrashLog() {
        try (FileReader fr = new FileReader(logFile)) {
            Properties properties = new Properties();
            properties.load(fr);
            return properties.getProperty("stack_trace");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void clearCrashLog() {
        //noinspection ResultOfMethodCallIgnored
        logFile.delete();
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
