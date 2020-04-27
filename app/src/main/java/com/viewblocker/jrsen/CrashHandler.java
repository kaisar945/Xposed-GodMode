package com.viewblocker.jrsen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.viewblocker.jrsen.injection.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by jrsen on 17-10-21.
 */

final class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static final String BUG_REPORT_DIR = "bug_report";
    private static final String BUG_REPORT_FILE = "crash_log.txt";

    static void init() {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        XposedBridge.log(e);
        Logger.e(TAG, "Crash", e);
        saveCrashLog(e);
        restart(BlockerApplication.getApplication());
    }

    private static File getCrashLogFile(Context context) {
        return new File(context.getExternalFilesDir(BUG_REPORT_DIR), BUG_REPORT_FILE);
    }

    private static void saveCrashLog(Throwable e) {
        FileOutputStream out = null;
        try {
            BlockerApplication app = BlockerApplication.getApplication();
            Properties properties = new Properties();
            properties.setProperty("stack_trace", Logger.getStackTraceString(e));
            out = new FileOutputStream(getCrashLogFile(app));
            properties.store(out, null);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException ignore) {
                }
        }
    }

    public static String getCrashLog() {
        FileInputStream in = null;
        try {
            BlockerApplication app = BlockerApplication.getApplication();
            Properties properties = new Properties();
            in = new FileInputStream(getCrashLogFile(app));
            properties.load(in);
            return properties.getProperty("stack_trace");
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException ignore) {
                }
        }
    }

    public static boolean handledCrashLog() {
        BlockerApplication app = BlockerApplication.getApplication();
        return getCrashLogFile(app).delete();
    }

    public static void restart(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent restartIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 5, restartIntent);
        //结束进程
        System.exit(1);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
