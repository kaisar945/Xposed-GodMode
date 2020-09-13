package com.viewblocker.jrsen.service;


import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.viewblocker.jrsen.IGodModeManager;
import com.viewblocker.jrsen.IObserver;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.injection.util.FileUtils;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.viewblocker.jrsen.BlockerApplication.TAG;
import static com.viewblocker.jrsen.injection.util.FileUtils.S_IRWXG;
import static com.viewblocker.jrsen.injection.util.FileUtils.S_IRWXO;
import static com.viewblocker.jrsen.injection.util.FileUtils.S_IRWXU;


/**
 * Created by jrsen on 17-10-15.
 * 上帝模式核心管理服务所有跨进程通讯均通过此服务
 * 该服务通过Xposed注入到SystemServer进程作为一个系统服务
 * Client端可以使用{@link GodModeManager#getDefault()}使用该服务提供的接口
 */

public final class GodModeManagerService extends IGodModeManager.Stub implements Handler.Callback {

    /* /data/godmode */
    private final static String BASE_DIR = String.format("%s/%s", Environment.getDataDirectory().getAbsolutePath(), "godmode");
    /* /data/godmode/conf */
    private final static String CONFIG_FILE_NAME = "conf";
    /* /data/godmode/com.tencent.mm/com.tencent.mm.rule */
    private final static String RULE_FILE_SUFFIX = ".rule";

    private static final int SET_EDIT_MODE = 0x00001;
    private static final int WRITE_RULE = 0x00002;
    private static final int DELETE_RULE = 0x00004;
    private static final int DELETE_RULES = 0x00008;
    private static final int UPDATE_RULE = 0x000016;

    public static final String CONF_EDIT_MODE = "edit_mode";

    private final RemoteCallbackList<ObserverProxy> remoteCallbackList = new RemoteCallbackList<>();
    private final HashMap<String, ActRules> ruleCache = new HashMap<>();
    private Handler handle;
    private boolean inEditMode;
    private boolean loadSuccess;

    public GodModeManagerService() {
        HandlerThread writerThread = new HandlerThread("writer-thread");
        writerThread.start();
        handle = new Handler(writerThread.getLooper(), this);
        loadSuccess = loadPreferenceData();
        //disable edit mode on system up
        setEditMode(false);
    }

    private boolean loadPreferenceData() {
        try {
            String configFilePath = getConfigFilePath();
            JSONObject jobject = new JSONObject(FileUtils.readTextFile(configFilePath, 0, null));
            inEditMode = jobject.optBoolean(CONF_EDIT_MODE, false);
            File dataDir = new File(getBaseDir());
            File[] packageDirs = dataDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            Objects.requireNonNull(packageDirs);
            HashMap<String, ActRules> appRules = new HashMap<>();
            for (File packageDir : packageDirs) {
                try {
                    String packageName = packageDir.getName();
                    String appRuleFile = getAppRuleFilePath(packageName);
                    String json = FileUtils.readTextFile(appRuleFile, 0, null);
                    ActRules rules = new Gson().fromJson(json, ActRules.class);
                    appRules.put(packageName, rules);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ruleCache.putAll(appRules);
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "start GodModeManagerService failed", e);
            return false;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case SET_EDIT_MODE: {
                try {
                    boolean enable = (boolean) msg.obj;
                    JSONObject jobject = new JSONObject();
                    jobject.put(CONF_EDIT_MODE, enable);
                    FileUtils.stringToFile(getConfigFilePath(), jobject.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case WRITE_RULE: {
                try {
                    Object[] args = (Object[]) msg.obj;
                    ActRules actRules = (ActRules) args[0];
                    String packageName = (String) args[1];
                    ViewRule viewRule = (ViewRule) args[2];
                    Bitmap snapshot = (Bitmap) args[3];
                    try {
                        if (Preconditions.checkBitmapOrThrow(snapshot)) {
                            String appDataDir = getAppDataDir(packageName);
                            viewRule.imagePath = saveBitmap(snapshot, appDataDir);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String json = new Gson().toJson(actRules);
                    String appRuleFilePath = getAppRuleFilePath(packageName);
                    FileUtils.stringToFile(appRuleFilePath, json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;
            case DELETE_RULE: {
                try {
                    Object[] args = (Object[]) msg.obj;
                    ActRules actRules = (ActRules) args[0];
                    String packageName = (String) args[1];
                    ViewRule viewRule = (ViewRule) args[2];
                    FileUtils.delete(viewRule.imagePath);
                    String json = new Gson().toJson(actRules);
                    FileUtils.stringToFile(getAppRuleFilePath(packageName), json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;
            case DELETE_RULES: {
                try {
                    String packageName = (String) msg.obj;
                    FileUtils.delete(getAppDataDir(packageName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case UPDATE_RULE: {
                try {
                    Object[] args = (Object[]) msg.obj;
                    ActRules actRules = (ActRules) args[0];
                    String packageName = (String) args[1];
                    String json = new Gson().toJson(actRules);
                    FileUtils.stringToFile(getAppRuleFilePath(packageName), json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            default: {
                //no implements
            }
            break;
        }
        return true;
    }

    /**
     * 设置是否显示编辑模式并且通知所有客户端状态改变
     * !!!该接口会进行身份校验仅允许上帝模式调用
     *
     * @param enable true:开启 false:关闭
     */
    @Override
    public void setEditMode(boolean enable) {
        inEditMode = enable;
        handle.obtainMessage(SET_EDIT_MODE, enable).sendToTarget();
        notifyObserverEditModeChanged(enable);
    }

    /**
     * 检查当前是否开启了编辑模式
     *
     * @return true:开启 false:关闭
     */
    @Override
    public boolean isInEditMode() {
        return inEditMode;
    }

    /**
     * 注册一个客户端当状态发生变化时通知客户端
     *
     * @param packageName 客户端包名
     * @param observer    监视器对象
     */
    @Override
    public void addObserver(String packageName, IObserver observer) {
//        if (!started) return;
        remoteCallbackList.register(new ObserverProxy(packageName, observer));
    }

    /**
     * 获取所有应用的规则
     * !!!该接口会进行身份校验仅允许上帝模式调用
     *
     * @return 所有应用规则
     */
    @Override
    public Map<String, ActRules> getAllRules() {
//        if (!started) return Collections.emptyMap();
        return ruleCache;
    }

    /**
     * 获取应用包名下的所有规则
     * !!!该接口会进行身份校验仅允许上帝模式和客户端自己调用
     *
     * @param packageName 应用包名
     * @return 应用规则
     */
    @Override
    public ActRules getRules(String packageName) {
//        if (!started) return new ActRules();
        return ruleCache.containsKey(packageName) ? ruleCache.get(packageName) : new ActRules();
    }

    /**
     * 写入规则
     *
     * @param packageName 规则包名
     * @param viewRule    规则
     * @param snapshot    控件快照
     */
    @Override
    public boolean writeRule(String packageName, ViewRule viewRule, Bitmap snapshot) {
//        if (!started) return false;
        try {
            ActRules actRules = ruleCache.get(packageName);
            if (actRules == null) {
                ruleCache.put(packageName, actRules = new ActRules());
            }
            List<ViewRule> viewRules = actRules.get(viewRule.activityClass);
            if (viewRules == null) {
                actRules.put(viewRule.activityClass, viewRules = new ArrayList<>());
            }
            viewRules.add(viewRule);
            handle.obtainMessage(WRITE_RULE, new Object[]{actRules, packageName, viewRule, snapshot}).sendToTarget();
            notifyObserverRuleChanged(packageName, actRules);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 更新规则
     *
     * @param packageName 应用包名
     * @param viewRule    规则
     * @return true:成功 false:失败
     */
    @Override
    public boolean updateRule(String packageName, ViewRule viewRule) {
//        if (!started) return false;
        try {
            ActRules actRules = ruleCache.get(packageName);
            if (actRules == null) {
                ruleCache.put(packageName, actRules = new ActRules());
            }
            List<ViewRule> viewRules = actRules.get(viewRule.activityClass);
            if (viewRules == null) {
                actRules.put(viewRule.activityClass, viewRules = new ArrayList<>());
            }
            int index = viewRules.indexOf(viewRule);
            if (index >= 0) {
                viewRules.set(index, viewRule);
            } else {
                viewRules.add(viewRule);
            }
            handle.obtainMessage(UPDATE_RULE, new Object[]{actRules, packageName}).sendToTarget();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除一条规则
     *
     * @param packageName 应用包名
     * @param viewRule    规则
     * @return true:成功 false:失败
     */
    @Override
    public boolean deleteRule(String packageName, ViewRule viewRule) {
//        if (!started) return false;
        try {
            ActRules actRules = Objects.requireNonNull(ruleCache.get(packageName), "not found this rule can't delete.");
            List<ViewRule> viewRules = Objects.requireNonNull(actRules.get(viewRule.activityClass), "not found this rule can't delete.");
            boolean removed = viewRules.remove(viewRule);
            if (removed) {
                handle.obtainMessage(DELETE_RULE, new Object[]{actRules, packageName, viewRule}).sendToTarget();
            }
            return removed;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除应用的所有规则
     *
     * @param packageName 应用包名
     * @return true:成功 false:失败
     */
    @Override
    public boolean deleteRules(String packageName) {
//        if (!started) return false;
        try {
            ruleCache.remove(packageName);
            handle.obtainMessage(DELETE_RULES, packageName).sendToTarget();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String saveBitmap(Bitmap bitmap, String dir) {
        try {
            File file = new File(dir, System.currentTimeMillis() + ".webp");
            try (FileOutputStream out = new FileOutputStream(file)) {
                if (bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)) {
                    FileUtils.setPermissions(file, S_IRWXU | S_IRWXG | S_IRWXO, -1, -1);
                    return file.getAbsolutePath();
                }
                throw new FileNotFoundException("bitmap can't compress to " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void notifyObserverRuleChanged(String packageName, ActRules actRules) {
        final int N = remoteCallbackList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                ObserverProxy observerProxy = remoteCallbackList.getBroadcastItem(i);
                if (TextUtils.equals(observerProxy.packageName, packageName)) {
                    observerProxy.observer.onViewRuleChanged(actRules);
                }
            } catch (RemoteException ignored) {
//                ignored.printStackTrace();
            }
        }
        remoteCallbackList.finishBroadcast();
    }

    private void notifyObserverEditModeChanged(boolean enable) {
        final int N = remoteCallbackList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                remoteCallbackList.getBroadcastItem(i).onEditModeChanged(enable);
            } catch (RemoteException ignored) {
//                ignored.printStackTrace();
            }
        }
        remoteCallbackList.finishBroadcast();
    }

    private String getBaseDir() throws FileNotFoundException {
        File dir = new File(BASE_DIR);
        if (dir.exists() || dir.mkdirs()) {
            FileUtils.setPermissions(dir, S_IRWXU | S_IRWXG | S_IRWXO, -1, -1);
            return dir.getAbsolutePath();
        }
        throw new FileNotFoundException();
    }

    private String getConfigFilePath() throws IOException {
        File file = new File(getBaseDir(), CONFIG_FILE_NAME);
        if (file.exists() || file.createNewFile()) {
            FileUtils.setPermissions(file, S_IRWXU | S_IRWXG | S_IRWXO, -1, -1);
            return file.getAbsolutePath();
        }
        throw new FileNotFoundException();
    }

    private String getAppDataDir(String packageName) throws FileNotFoundException {
        File dir = new File(getBaseDir(), packageName);
        if (dir.exists() || dir.mkdirs()) {
            FileUtils.setPermissions(dir, S_IRWXU | S_IRWXG | S_IRWXO, -1, -1);
            return dir.getAbsolutePath();
        }
        throw new FileNotFoundException();
    }

    private String getAppRuleFilePath(String packageName) throws IOException {
        File file = new File(getAppDataDir(packageName), packageName + RULE_FILE_SUFFIX);
        if (file.exists() || file.createNewFile()) {
            FileUtils.setPermissions(file, S_IRWXU | S_IRWXG | S_IRWXO, -1, -1);
            return file.getAbsolutePath();
        }
        throw new FileNotFoundException();
    }

    private static final class ObserverProxy implements IObserver {

        private final String packageName;
        private final IObserver observer;

        public ObserverProxy(String packageName, IObserver observer) {
            this.packageName = packageName;
            this.observer = observer;
        }

        @Override
        public void onEditModeChanged(boolean enable) throws RemoteException {
            observer.onEditModeChanged(enable);
        }

        @Override
        public void onViewRuleChanged(ActRules actRules) throws RemoteException {
            observer.onViewRuleChanged(actRules);
        }

        @Override
        public IBinder asBinder() {
            return observer.asBinder();
        }
    }

}
