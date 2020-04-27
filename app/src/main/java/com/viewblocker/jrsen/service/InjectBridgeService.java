package com.viewblocker.jrsen.service;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.viewblocker.jrsen.IClientReceiver;
import com.viewblocker.jrsen.InjectBridge;
import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.database.ViewRulesTable;
import com.viewblocker.jrsen.fragment.GeneralPreferenceFragment;
import com.viewblocker.jrsen.injection.bridge.LocalInjectBridge;
import com.viewblocker.jrsen.injection.util.FileUtils;
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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jrsen on 17-10-14.
 */

public final class InjectBridgeService extends InjectBridge.Stub {

    private static final String TAG = "InjectBridgeService";
    private static InjectBridgeService _instance;
    private final String HOST_BASE_DATA_DIR;//such; /data/data/com.viewblocker.jrsen
    private final Context context;


    public static InjectBridgeService getBridge(Context context) {
        if (_instance == null) {
            synchronized (InjectBridgeService.class) {
                if (_instance == null) {
                    _instance = new InjectBridgeService(context);
                }
            }
        }
        return _instance;
    }

    private InjectBridgeService(Context context) {
        this.context = context.getApplicationContext();
        ApplicationInfo aInfo = context.getApplicationInfo();
        HOST_BASE_DATA_DIR = aInfo.dataDir;
        FileUtils.setPermissions(HOST_BASE_DATA_DIR, 0757, -1, -1);

        String hostDataDir = LocalInjectBridge.getHostDataDir(HOST_BASE_DATA_DIR);
        String configFilePath = LocalInjectBridge.getConfigFilePath(HOST_BASE_DATA_DIR);

        if (!FileUtils.exists(hostDataDir)) {
            FileUtils.mkdirs(hostDataDir, 0777);
        }

        if (FileUtils.exists(hostDataDir) && !FileUtils.exists(configFilePath)) {
            FileUtils.createNewFile(configFilePath, 0774);
            setEditModeEnable(isInEditMode());
            migrateLegacySettingData();
        }
    }

    public boolean fixHostDirPermission() {
        String hostDataDir = LocalInjectBridge.getHostDataDir(HOST_BASE_DATA_DIR);
        if (FileUtils.exists(hostDataDir))
            FileUtils.setPermissions(hostDataDir, 0777, -1, -1);
        String configFilePath = LocalInjectBridge.getConfigFilePath(HOST_BASE_DATA_DIR);
        if (FileUtils.exists(configFilePath))
            FileUtils.setPermissions(configFilePath, 0774, -1, -1);
        return FileUtils.exists(HOST_BASE_DATA_DIR)
                && (FileUtils.setPermissions(HOST_BASE_DATA_DIR, 0757, -1, -1) == 0);
    }

    /**
     * 迁移旧版本的数据
     */
    private void migrateLegacySettingData() {
        HashMap<String, ActRules> appRules = ViewRulesTable.getAppRules(context);
        Set<String> packageNames = appRules.keySet();
        for (String packageName : packageNames) {
            String thirdAppDataDir = LocalInjectBridge.getAppHomeDataDir(HOST_BASE_DATA_DIR, packageName);
            String thirdAppRuleFilePath = LocalInjectBridge.getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
            if (!FileUtils.exists(thirdAppDataDir)
                    && FileUtils.mkdirs(thirdAppDataDir, 0777)
                    && FileUtils.createNewFile(thirdAppRuleFilePath, 0777)) {
                ActRules actRules = appRules.get(packageName);
                Collection<List<ViewRule>> values = actRules.values();
                for (List<ViewRule> rules : values) {
                    for (ViewRule viewRule : rules) {
                        File src = new File(viewRule.snapshotFilePath);
                        File dst = new File(thirdAppDataDir, src.getName());
                        if (FileUtils.copy(src, dst)) {
                            FileUtils.delete(viewRule.snapshotFilePath);
                            viewRule.snapshotFilePath = dst.getAbsolutePath();
                        }
                    }
                }
                try {
                    FileUtils.stringToFile(thirdAppRuleFilePath, new Gson().toJson(actRules));
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean isInEditMode() {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(GeneralPreferenceFragment.KEY_EDITOR_SWITCH, false);
    }

    public void setEditModeEnable(boolean enable) {
        try {
            String configFilePath = LocalInjectBridge.getConfigFilePath(HOST_BASE_DATA_DIR);
            String json = FileUtils.readTextFile(configFilePath, 0, null);
            FileOutputStream out = new FileOutputStream(configFilePath);
            try {
                JSONObject jobject;
                try {
                    jobject = new JSONObject(json);
                } catch (JSONException ignore) {
                    jobject = new JSONObject();
                }
                jobject.put(LocalInjectBridge.CONF_GLOBAL_SWITCH, enable);
                out.write(jobject.toString().getBytes());
                out.flush();
            } finally {
                out.close();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(context, enable ? R.string.toast_tip_edit_enable : R.string.toast_tip_edit_disable,
                enable ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    @Override
    public void registerReceiver(String packageName, IClientReceiver receiver) {
        // Empty implement
    }

    @Override
    public ActRules getRules(String packageName) {
        try {
            String thirdAppRuleFilePath = LocalInjectBridge.getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
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
//                                Bitmap viewThumbnail = v276ViewRule.viewThumbnail;
                                String act_class = v276ViewRule.activityClassName;
                                String view_class = v276ViewRule.viewClassName;
                                int[] view_hierarchy_depth = v276ViewRule.viewHierarchyDepth;
                                String res_name = v276ViewRule.resourceName;
                                int visibility = v276ViewRule.visibility;
                                long time_stamp = v276ViewRule.recordTimeStamp;
                                ViewRule viewRule = new ViewRule(viewThumbnailFilePath, alias, x, y, width, height, act_class, view_class, view_hierarchy_depth, res_name, visibility, time_stamp);
                                viewRules.add(viewRule);
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
//                                Bitmap viewThumbnail = v275ViewRule.h;
                                String act_class = v275ViewRule.i;
                                String view_class = v275ViewRule.j;
                                int[] view_hierarchy_depth = v275ViewRule.k;
                                String res_name = v275ViewRule.l;
                                int visibility = v275ViewRule.m;
                                long time_stamp = v275ViewRule.n;
                                ViewRule viewRule = new ViewRule(viewThumbnailFilePath, alias, x, y, width, height, act_class, view_class, view_hierarchy_depth, res_name, visibility, time_stamp);
                                viewRules.add(viewRule);
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
//                                Bitmap viewThumbnail = v274ViewRule.g;
                                String act_class = v274ViewRule.h;
                                String view_class = v274ViewRule.i;
                                int[] view_hierarchy_depth = v274ViewRule.j;
                                String res_name = v274ViewRule.k;
                                int visibility = v274ViewRule.l;
                                long time_stamp = v274ViewRule.m;
                                ViewRule viewRule = new ViewRule(viewThumbnailFilePath, "", x, y, width, height, act_class, view_class, view_hierarchy_depth, res_name, visibility, time_stamp);
                                viewRules.add(viewRule);
                            }
                            actRules.put(entry.getKey(), viewRules);
                        }
                        return actRules;
                    }
                }
            }
        } catch (JSONException ignored) {
//            ignored.printStackTrace();
        }
        return new Gson().fromJson(json, ActRules.class);
    }

    public HashMap<String, ActRules> getAppRules() {
        String hostDataDir = LocalInjectBridge.getHostDataDir(HOST_BASE_DATA_DIR);
        File dataDir = new File(hostDataDir);
        File[] thirdPackageDirs = dataDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        HashMap<String, ActRules> appRules = new HashMap<>();
        if (thirdPackageDirs != null && thirdPackageDirs.length > 0) {
            for (File file : thirdPackageDirs) {
                String packageName = file.getName();
                ActRules rules = getRules(packageName);
                if (rules == null || rules.isEmpty()) {
                    continue;
                }
                appRules.put(packageName, rules);
            }
        }
        return appRules;
    }

    @Override
    public void writeRule(String packageName, ViewRule viewRule, Bitmap snapshot) {
        writeRuleInternal(packageName, viewRule, snapshot);
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
            String thirdAppDataDir = LocalInjectBridge.getAppHomeDataDir(HOST_BASE_DATA_DIR, packageName);
            viewRule.snapshotFilePath = LocalInjectBridge.writeBitmapToLocal(snapshot, thirdAppDataDir);
            //规则写到remote则把client的缩略图删了 不然容易OOM
            snapshot.recycle();
        }

        try {
            String json = new Gson().toJson(actRules);
            String thirdAppRuleFilePath = LocalInjectBridge.getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
            FileUtils.stringToFile(thirdAppRuleFilePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteRule(String packageName, ViewRule viewRule) {
        ActRules rules = getRules(packageName);
        List<ViewRule> list = rules.get(viewRule.activityClassName);
        if (list != null && !list.isEmpty()) {
            FileUtils.delete(viewRule.snapshotFilePath);
            list.remove(viewRule);
            if (list.isEmpty())
                rules.remove(viewRule.activityClassName);
            try {
                String thirdAppRuleFilePath = LocalInjectBridge.getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
                FileUtils.stringToFile(thirdAppRuleFilePath, new Gson().toJson(rules));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean purgeRulesByPackage(String packageName) {
        ActRules rules = getRules(packageName);
        for (List<ViewRule> list : rules.values()) {
            for (ViewRule viewRule : list) {
                FileUtils.delete(viewRule.snapshotFilePath);
            }
        }
        try {
            String thirdAppRuleFilePath = LocalInjectBridge.getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
            FileUtils.stringToFile(thirdAppRuleFilePath, new JSONArray(Collections.emptyList()).toString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean update(Context context, String packageName, ViewRule viewRule) {
        ActRules rules = getRules(packageName);
        if (rules == null) return false;
        List<ViewRule> list = rules.get(viewRule.activityClassName);
        if (list == null) return false;
        int index = list.indexOf(viewRule);
        list.set(index, viewRule);
        try {
            String thirdAppRuleFilePath = LocalInjectBridge.getAppRuleFilePath(HOST_BASE_DATA_DIR, packageName);
            FileUtils.stringToFile(thirdAppRuleFilePath, new Gson().toJson(rules));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
