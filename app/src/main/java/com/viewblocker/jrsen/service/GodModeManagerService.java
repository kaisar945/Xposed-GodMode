package com.viewblocker.jrsen.service;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.google.gson.Gson;
import com.viewblocker.jrsen.IGodModeManager;
import com.viewblocker.jrsen.IObserver;
import com.viewblocker.jrsen.injection.util.FileUtils;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jrsen on 17-10-15.
 * <p>
 * running in system_server process as an SystemServer
 */

public final class GodModeManagerService extends IGodModeManager.Stub implements Handler.Callback {

    /* /sdcard/Android/data/com.viewblocker.jrsen/ */
    private final static String BASE_DIR = "/sdcard/Android/data/com.viewblocker.jrsen";
    /* /sdcard/Android/data/com.viewblocker.jrsen/app_data */
    private final static String DATA_DIR = "app_data";
    /* /sdcard/Android/data/com.viewblocker.jrsen/app_data/conf */
    private final static String CONFIG_FILE_NAME = "conf";
    /* /sdcard/Android/data/com.viewblocker.jrsen/app_data/com.tencent.mm.rule */
    private final static String RULE_FILE_SUFFIX = ".rule";

    private final static String EXTRA_PACKAGE_NAME = "app.intent.extra.PACKAGE_NAME";
    private final static String EXTRA_VIEW_RULE = "app.intent.extra.VIEW_RULE";
    private final static String EXTRA_SNAPSHOT = "app.intent.extra.SNAPSHOT";

    public final static String CONF_GLOBAL_SWITCH = "global_switch";

    private final Handler handle;
    private final RemoteCallbackList<IObserver> observerRemoteCallbackList = new RemoteCallbackList<>();
    private final ActRules actRules = new ActRules();
    private final HashMap<String, ActRules> ruleCache = new HashMap<>();
    private boolean inEditMode;

    public GodModeManagerService() {
        HandlerThread writerThread = new HandlerThread("writer-thread");
        writerThread.start();
        handle = new Handler(writerThread.getLooper(), this);
        loadRuleDataSync();
    }

    private void loadRuleDataSync() {
        File dataDir = new File(getDataDir());
        File[] packageDirs = dataDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        HashMap<String, ActRules> appRules = new HashMap<>();
        if (packageDirs != null && packageDirs.length > 0) {
            for (File dir : packageDirs) {
                String packageName = dir.getName();
                String appRuleFile = getAppRuleFilePath(packageName);
                String json;
                try {
                    json = FileUtils.readTextFile(appRuleFile, 0, null);
                } catch (IOException e) {
                    continue;
                }
                ActRules rules = new Gson().fromJson(json, ActRules.class);
                if (rules == null || rules.isEmpty()) {
                    continue;
                }
                appRules.put(packageName, rules);
            }
        }
        ruleCache.putAll(appRules);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Bundle extras = msg.getData();
        String packageName = extras.getString(EXTRA_PACKAGE_NAME);
        ViewRule viewRule = extras.getParcelable(EXTRA_VIEW_RULE);
        Bitmap snapshot = extras.getParcelable(EXTRA_SNAPSHOT);
        writeRuleInternal(packageName, viewRule, snapshot);
        return true;
    }

    @Override
    public void setEditMode(boolean enable) {
        inEditMode = enable;
        try {
            String configFilePath = getConfigFilePath();
            String json = FileUtils.readTextFile(configFilePath, 0, null);
            try (FileOutputStream out = new FileOutputStream(configFilePath)) {
                JSONObject jobject = new JSONObject(json);
                jobject.put(GodModeManagerService.CONF_GLOBAL_SWITCH, enable);
                out.write(jobject.toString().getBytes());
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isInEditMode() {
        return inEditMode;
    }

    @Override
    public void addObserver(String packageName, IObserver observer) {
        observerRemoteCallbackList.register(observer);
    }

    @Override
    public ActRules getRules(String packageName) {
        return ruleCache.containsKey(packageName) ? ruleCache.get(packageName) : new ActRules();
    }

    @Override
    public void writeRule(String packageName, ViewRule viewRule, Bitmap snapshot) {
        Message message = handle.obtainMessage();
        Bundle extras = new Bundle();
        extras.putString(EXTRA_PACKAGE_NAME, packageName);
        extras.putParcelable(EXTRA_VIEW_RULE, viewRule);
        extras.putParcelable(EXTRA_SNAPSHOT, snapshot);
        message.setData(extras);
        message.sendToTarget();
        ActRules actRules = ruleCache.get(packageName);
        List<ViewRule> viewRules = actRules.get(viewRule.activityClassName);
        viewRules.add(viewRule);
//        notifyRuleChanged(packageName, );
    }

    private void writeRuleInternal(String packageName, ViewRule viewRule, Bitmap snapshot) {
        ActRules actRules = getRules(packageName);
        if (actRules == null) actRules = new ActRules();
        List<ViewRule> viewRules = actRules.get(viewRule.activityClassName);
        if (viewRules == null) {
            viewRules = new ArrayList<>();
            actRules.put(viewRule.activityClassName, viewRules);
        }
        viewRules.add(viewRule);

        //save thumbnail
        if (Preconditions.checkBitmap(snapshot)) {
            String thirdAppDataDir = getAppHomeDataDir(packageName);
            viewRule.snapshotFilePath = saveBitmap(snapshot, thirdAppDataDir);
            //规则写到remote则把client的缩略图删了 不然容易OOM
            snapshot.recycle();
        }

        try {
            String json = new Gson().toJson(actRules);
            String thirdAppRuleFilePath = getAppRuleFilePath(packageName);
            FileUtils.stringToFile(thirdAppRuleFilePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String saveBitmap(Bitmap bitmap, String dataDir) {
        try {
            File file = new File(dataDir, System.currentTimeMillis() + ".webp");
            try (FileOutputStream out = new FileOutputStream(file)) {
                return bitmap.compress(Bitmap.CompressFormat.WEBP, 10, out) ? file.getAbsolutePath() : null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void notifyRuleChanged(String packageName, ActRules actRules) {
        final int N = observerRemoteCallbackList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                observerRemoteCallbackList.getBroadcastItem(i).onViewRuleChanged(actRules);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyEditModeChanged(boolean inEditMode) {
        final int N = observerRemoteCallbackList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                observerRemoteCallbackList.getBroadcastItem(i).onEditModeChanged(inEditMode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDataDir() {
        File path = new File(BASE_DIR, DATA_DIR);
        if (path.exists() || path.mkdirs()) {
            return path.getAbsolutePath();
        }
        return null;
    }

    public static String getConfigFilePath() {
        File file = new File(getDataDir(), CONFIG_FILE_NAME);
        try {
            if (file.exists() || file.createNewFile()) {
                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getAppHomeDataDir(String packageName) {
        File path = new File(getDataDir(), packageName);
        if (path.exists() || path.mkdirs()) {
            return path.getAbsolutePath();
        }
        return null;
    }

    public static String getAppRuleFilePath(String packageName) {
        File file = new File(getAppHomeDataDir(packageName), packageName + RULE_FILE_SUFFIX);
        try {
            if (file.exists() || file.createNewFile()) {
                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
