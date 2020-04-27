package com.viewblocker.jrsen.injection.bridge;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.viewblocker.jrsen.IClientReceiver;
import com.viewblocker.jrsen.InjectBridge;
import com.viewblocker.jrsen.injection.BlockerInjector;
import com.viewblocker.jrsen.injection.util.FileUtils;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.rule.ViewRule_V274;
import com.viewblocker.jrsen.rule.ViewRule_V275;
import com.viewblocker.jrsen.rule.ViewRule_V276;
import com.viewblocker.jrsen.util.Preconditions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.viewblocker.jrsen.BlockerApplication.TAG;

/**
 * Created by jrsen on 17-10-15.
 */

public final class LocalInjectBridge extends InjectBridge.Stub implements Handler.Callback {

    @SuppressLint("StaticFieldLeak")
    private static LocalInjectBridge _instance;
    private final String HOST_BASE_DATA_DIR = "/data/data/com.viewblocker.jrsen";//such: /data/data/com.viewblocker.jrsen
    private final static String DATA_DIR = "app_data";//such: /data/data/com.viewblocker.jrsen/app_data
    private final static String CONFIG_FILE_NAME = "conf";//such: /data/data/com.viewblocker.jrsen/app_data/conf
    private final static String RULE_FILE_NAME = ".rule"; //such: /data/data/com.viewblocker.jrsen/app_data/com.tencent.mm.rule

    private final static String EXTRA_PACKAGE_NAME = "app.intent.extra.PACKAGE_NAME";
    private final static String EXTRA_VIEW_RULE = "app.intent.extra.VIEW_RULE";
    private final static String EXTRA_SNAPSHOT = "app.intent.extra.SNAPSHOT";

    public final static String CONF_GLOBAL_SWITCH = "global_switch";

    private final Handler handle;
    private final String packageName;
    private IClientReceiver mClientReceiver;
    @SuppressWarnings("FieldCanBeLocal")
    private FileObserver confFileObserver, appRuleFileObserver;

    public static LocalInjectBridge initialize(String packageName) {
        if (_instance == null) {
            synchronized (LocalInjectBridge.class) {
                if (_instance == null) {
                    _instance = new LocalInjectBridge(packageName);
                }
            }
        }
        return _instance;
    }

    public static LocalInjectBridge getBridge() {
        return _instance;
    }

    private LocalInjectBridge(final String packageName) {
        this.packageName = packageName;
        HandlerThread writerThread = new HandlerThread("writer-thread");
        writerThread.start();
        handle = new Handler(writerThread.getLooper(), this);
        String configFilePath = getConfigFilePath(HOST_BASE_DATA_DIR);
        String appRuleFilePath = getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
        if (FileUtils.exists(configFilePath) && FileUtils.exists(appRuleFilePath)) {
            confFileObserver = new FileObserver(configFilePath, FileObserver.CLOSE_WRITE) {

                @Override
                public void onEvent(int event, @Nullable String path) {
                    Logger.d(TAG, "ConfFileHasChanged=" + isInEditMode() + " process name:" + BlockerInjector.loadPackageParam.packageName);
                    if (mClientReceiver != null) {
                        try {
                            mClientReceiver.editModeStateChanged(isInEditMode());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            confFileObserver.startWatching();
            appRuleFileObserver = new FileObserver(appRuleFilePath, FileObserver.CLOSE_WRITE) {

                @Override
                public void onEvent(int event, @Nullable String path) {
                    Logger.d(TAG, "RuleFileHasChanged");
                    if (mClientReceiver != null) {
                        try {
                            mClientReceiver.viewRulesHasChanged(getRules(packageName));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            appRuleFileObserver.startWatching();
        }
    }

    public static String getHostDataDir(String hostBaseDataDir) {
        File path = new File(hostBaseDataDir, DATA_DIR);
        if (path.exists()) {
            FileUtils.setPermissions(path, 0777, -1, -1);
        } else {
            FileUtils.mkdirs(path.getAbsolutePath(), 0777);
        }
        return path.getAbsolutePath();
    }

    public static String getConfigFilePath(String hostBaseDataDir) {
        File file = new File(getHostDataDir(hostBaseDataDir), CONFIG_FILE_NAME);
        if (file.exists()) {
            FileUtils.setPermissions(file, 0777, -1, -1);
        } else {
            FileUtils.createNewFile(file.getAbsolutePath(), 0774);
        }
        return file.getAbsolutePath();
    }

    public static String getAppHomeDataDir(String hostBaseDataDir, String thirdPackageName) {
        File path = new File(getHostDataDir(hostBaseDataDir), thirdPackageName);
        if (path.exists()) {
            FileUtils.setPermissions(path, 0777, -1, -1);
        } else {
            FileUtils.mkdirs(path.getAbsolutePath(), 0777);
        }
        return path.getAbsolutePath();
    }

    public static String getAppRuleFilePath(String hostBaseDataDir, String packageName) {
        File file = new File(getAppHomeDataDir(hostBaseDataDir, packageName), packageName + RULE_FILE_NAME);
        if (file.exists()) {
            FileUtils.setPermissions(file, 0777, -1, -1);
        } else {
            FileUtils.createNewFile(file.getAbsolutePath(), 0777);
        }
        return file.getAbsolutePath();
    }

    @Override
    public boolean isInEditMode() {
        try {
            String filePath = getConfigFilePath(HOST_BASE_DATA_DIR);
            String json = FileUtils.readTextFile(filePath, 0, null);
            JSONObject jobject = new JSONObject(json);
            return jobject.optBoolean(CONF_GLOBAL_SWITCH, false);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void registerReceiver(String packageName, IClientReceiver receiver) {
        mClientReceiver = receiver;
    }

    @Override
    public ActRules getRules(String packageName) {
        try {
            String thirdAppRuleFilePath = getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
            String json = FileUtils.readTextFile(thirdAppRuleFilePath, 0, null);
            return fromJsonCompat(json);
        } catch (Exception ignore) {
            return new ActRules();
        }
    }

    private ActRules fromJsonCompat(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.length() > 0) {
                String name = jsonObject.keys().next();
                JSONArray jsonArray = jsonObject.optJSONArray(name);
                if (jsonArray.length() > 0) {
                    JSONObject viewRuleJsonObject = jsonArray.optJSONObject(0);
                    ActRules actRules = new ActRules();
                    if (viewRuleJsonObject.has("recordTimeStamp")) {//276版本数据
                        Type type = new TypeToken<HashMap<String, List<ViewRule_V276>>>() {
                        }.getType();
                        HashMap<String, List<ViewRule_V276>> v276ActRuleMap = new Gson().fromJson(json, type);
                        for (Map.Entry<String, List<ViewRule_V276>> entry : v276ActRuleMap.entrySet()) {
                            List<ViewRule_V276> v276ViewRules = entry.getValue();
                            if (v276ViewRules == null || v276ViewRules.isEmpty()) {
                                continue;
                            }
                            List<ViewRule> viewRules = new ArrayList<>(v276ViewRules.size());
                            for (ViewRule_V276 v276ViewRule : v276ViewRules) {
                                String viewThumbnailFilePath = v276ViewRule.viewThumbnailFilePath;
                                int x = v276ViewRule.x;
                                int y = v276ViewRule.y;
                                int width = v276ViewRule.width;
                                int height = v276ViewRule.height;
                                String alias = v276ViewRule.alias;
                                String act_class = v276ViewRule.activityClassName;
                                String view_class = v276ViewRule.viewClassName;
                                int[] view_hierarchy_depth = v276ViewRule.viewHierarchyDepth;
                                String res_name = v276ViewRule.resourceName;
                                int visibility = v276ViewRule.visibility;
                                long time_stamp = v276ViewRule.recordTimeStamp;
                                viewRules.add(new ViewRule(viewThumbnailFilePath, alias, x, y, width
                                        , height, act_class, view_class, view_hierarchy_depth, res_name, visibility, time_stamp));
                            }
                            actRules.put(entry.getKey(), viewRules);
                        }
                        return actRules;
                    } else if (viewRuleJsonObject.has("n")) {//275版本数据
                        Type type = new TypeToken<HashMap<String, List<ViewRule_V275>>>() {
                        }.getType();
                        HashMap<String, List<ViewRule_V275>> v275ActRuleMap = new Gson().fromJson(json, type);
                        for (Map.Entry<String, List<ViewRule_V275>> entry : v275ActRuleMap.entrySet()) {
                            List<ViewRule_V275> v275ViewRules = entry.getValue();
                            if (v275ViewRules == null || v275ViewRules.isEmpty()) {
                                continue;
                            }
                            List<ViewRule> viewRules = new ArrayList<>(v275ViewRules.size());
                            for (ViewRule_V275 v275ViewRule : v275ViewRules) {
                                String viewThumbnailFilePath = v275ViewRule.b;
                                String alias = v275ViewRule.c;
                                int x = v275ViewRule.d;
                                int y = v275ViewRule.e;
                                int width = v275ViewRule.f;
                                int height = v275ViewRule.g;
                                String act_class = v275ViewRule.i;
                                String view_class = v275ViewRule.j;
                                int[] view_hierarchy_depth = v275ViewRule.k;
                                String res_name = v275ViewRule.l;
                                int visibility = v275ViewRule.m;
                                long time_stamp = v275ViewRule.n;
                                viewRules.add(new ViewRule(viewThumbnailFilePath, alias, x, y, width
                                        , height, act_class, view_class, view_hierarchy_depth, res_name, visibility, time_stamp));
                            }
                            actRules.put(entry.getKey(), viewRules);
                        }
                        return actRules;
                    } else if (viewRuleJsonObject.has("m")) {//274版本数据
                        Type type = new TypeToken<HashMap<String, List<ViewRule_V274>>>() {
                        }.getType();
                        HashMap<String, List<ViewRule_V274>> v274ActRuleMap = new Gson().fromJson(json, type);
                        for (Map.Entry<String, List<ViewRule_V274>> entry : v274ActRuleMap.entrySet()) {
                            List<ViewRule_V274> v274ViewRules = entry.getValue();
                            if (v274ViewRules == null || v274ViewRules.isEmpty()) {
                                continue;
                            }
                            List<ViewRule> viewRules = new ArrayList<>(v274ViewRules.size());
                            for (ViewRule_V274 v274ViewRule : v274ViewRules) {
                                String viewThumbnailFilePath = v274ViewRule.b;
                                int x = v274ViewRule.c;
                                int y = v274ViewRule.d;
                                int width = v274ViewRule.e;
                                int height = v274ViewRule.f;
                                String act_class = v274ViewRule.h;
                                String view_class = v274ViewRule.i;
                                int[] view_hierarchy_depth = v274ViewRule.j;
                                String res_name = v274ViewRule.k;
                                int visibility = v274ViewRule.l;
                                long time_stamp = v274ViewRule.m;
                                viewRules.add(new ViewRule(viewThumbnailFilePath, "", x, y
                                        , width, height, act_class, view_class, view_hierarchy_depth, res_name, visibility, time_stamp));
                            }
                            actRules.put(entry.getKey(), viewRules);
                        }
                        return actRules;
                    }
                }
            }
        } catch (Exception ignore) {
//            ignore.printStackTrace();
        }
        return new Gson().fromJson(json, ActRules.class);
    }

    @Override
    public void writeRule(String packageName, ViewRule viewRule, Bitmap snapshot) {
        Message message = handle.obtainMessage(0x00001);
        Bundle extras = new Bundle();
        extras.putString(EXTRA_PACKAGE_NAME, packageName);
        extras.putParcelable(EXTRA_VIEW_RULE, viewRule);
        extras.putParcelable(EXTRA_SNAPSHOT, snapshot);
        message.setData(extras);
        message.sendToTarget();
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
            String thirdAppDataDir = getAppHomeDataDir(HOST_BASE_DATA_DIR, packageName);
            viewRule.snapshotFilePath = writeBitmapToLocal(snapshot, thirdAppDataDir);
            //规则写到remote则把client的缩略图删了 不然容易OOM
            snapshot.recycle();
        }

        try {
            String json = new Gson().toJson(actRules);
            String thirdAppRuleFilePath = getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
            FileUtils.stringToFile(thirdAppRuleFilePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String writeBitmapToLocal(Bitmap bitmap, String dataDir) {
        try {
            File file = new File(dataDir, System.currentTimeMillis() + ".webp");
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.WEBP, 10, out))
                FileUtils.setPermissions(file, 0776, -1, -1);
            return file.exists() ? file.getAbsolutePath() : null;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == 0x00001) {
            Bundle extras = msg.getData();
            String packageName = extras.getString(EXTRA_PACKAGE_NAME);
            ViewRule viewRule = extras.getParcelable(EXTRA_VIEW_RULE);
            Bitmap snapshot = extras.getParcelable(EXTRA_SNAPSHOT);
            writeRuleInternal(packageName, viewRule, snapshot);
        }
        return true;
    }

    @Override
    public boolean isBinderAlive() {
        return true;
    }

}
