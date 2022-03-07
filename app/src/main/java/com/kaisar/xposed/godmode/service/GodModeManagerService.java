package com.kaisar.xposed.godmode.service;


import static com.kaisar.xposed.godmode.injection.util.FileUtils.S_IRWXG;
import static com.kaisar.xposed.godmode.injection.util.FileUtils.S_IRWXO;
import static com.kaisar.xposed.godmode.injection.util.FileUtils.S_IRWXU;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.IGodModeManager;
import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.Preconditions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Created by jrsen on 17-10-15.
 * 上帝模式核心管理服务所有跨进程通讯均通过此服务
 * 该服务通过Xposed注入到SystemServer进程作为一个系统服务
 * Client端可以使用{@link GodModeManager#getDefault()}使用该服务提供的接口
 */

public final class GodModeManagerService extends IGodModeManager.Stub implements Handler.Callback {

    // /data/system/godmode
    private static final String BASE_DIR = String.format("%s/system/%s", Environment.getDataDirectory().getAbsolutePath(), "godmode");
    // /data/system/godmode/conf
    private static final String CONFIG_FILE_NAME = "conf";
    // /data/system/godmode/{package}/package.rule
    private static final String RULE_FILE_SUFFIX = ".rule";
    // /data/system/godmode/{package}/xxxxxxxxx.webp
    private static final String IMAGE_FILE_SUFFIX = ".webp";

    private static final int WRITE_RULE = 0x00002;
    private static final int DELETE_RULE = 0x00004;
    private static final int DELETE_RULES = 0x00008;
    private static final int UPDATE_RULE = 0x000016;

    private final Logger mLogger;
    private final RemoteCallbackList<ObserverProxy> mRemoteCallbackList = new RemoteCallbackList<>();
    private final AppRules mAppRulesCache = new AppRules();
    private final Context mContext;
    private final Handler mHandle;
    private boolean mInEditMode;
    private boolean mStarted;

    public GodModeManagerService(Context context) {
        mLogger = Logger.getLogger("GMMService");
        mContext = context;
        HandlerThread workThread = new HandlerThread("work-thread");
        workThread.start();
        mHandle = new Handler(workThread.getLooper(), this);
        try {
            loadRuleData();
            mStarted = true;
        } catch (Exception e) {
            mStarted = false;
            mLogger.e("loadPreferenceData failed " + BASE_DIR, e);
        }
    }

    private void loadRuleData() throws IOException {
        File dataDir = new File(getBaseDir());
        File[] packageDirs = dataDir.listFiles(File::isDirectory);
        if (packageDirs != null && packageDirs.length > 0) {
            HashMap<String, ActRules> appRules = new HashMap<>();
            for (File packageDir : packageDirs) {
                try {
                    String packageName = packageDir.getName();
                    String appRuleFile = getAppRuleFilePath(packageName);
                    String json = FileUtils.readTextFile(appRuleFile, 0, null);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    ActRules rules = gson.fromJson(json, ActRules.class);
                    Preconditions.checkNotNull(rules, "rules is null");
                    //compact rule
                    Iterator<Map.Entry<String, List<ViewRule>>> iterator = rules.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, List<ViewRule>> listEntry = iterator.next();
                        List<ViewRule> value = listEntry.getValue();
                        if (value == null || value.isEmpty()) {
                            iterator.remove();
                        }
                    }
                    if (rules.isEmpty()) {
                        FileUtils.delete(packageDir);
                        continue;
                    }
                    appRules.put(packageName, rules);
                } catch (IOException e) {
                    mLogger.w("load rule fail", e);
                } catch (NullPointerException | JsonSyntaxException e) {
                    mLogger.e("load rule error", e);
                    FileUtils.delete(packageDir);
                }
            }
            mAppRulesCache.putAll(appRules);
            mLogger.d("app rules cache=" + mAppRulesCache.size());
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WRITE_RULE: {
                try {
                    Object[] args = (Object[]) msg.obj;
                    ActRules actRules = (ActRules) args[0];
                    String packageName = (String) args[1];
                    ViewRule viewRule = (ViewRule) args[2];
                    Bitmap snapshot = (Bitmap) args[3];
                    String appDataDir = getAppDataDir(packageName);
                    viewRule.imagePath = saveBitmap(snapshot, appDataDir);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String json = gson.toJson(actRules);
                    String appRuleFilePath = getAppRuleFilePath(packageName);
                    FileUtils.stringToFile(appRuleFilePath, json);
                    notifyObserverRuleChanged(packageName, actRules);
                } catch (IOException e) {
                    mLogger.w("write rule failed", e);
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
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String json = gson.toJson(actRules);
                    FileUtils.stringToFile(getAppRuleFilePath(packageName), json);
                    notifyObserverRuleChanged(packageName, actRules);
                } catch (IOException e) {
                    mLogger.w("delete rule failed", e);
                }
            }
            break;
            case DELETE_RULES: {
                try {
                    String packageName = (String) msg.obj;
                    FileUtils.delete(getAppDataDir(packageName));
                    notifyObserverRuleChanged(packageName, new ActRules());
                } catch (FileNotFoundException e) {
                    mLogger.w("delete rules failed", e);
                }
            }
            break;
            case UPDATE_RULE: {
                try {
                    Object[] args = (Object[]) msg.obj;
                    ActRules actRules = (ActRules) args[0];
                    String packageName = (String) args[1];
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String json = gson.toJson(actRules);
                    FileUtils.stringToFile(getAppRuleFilePath(packageName), json);
                    notifyObserverRuleChanged(packageName, actRules);
                } catch (IOException e) {
                    mLogger.w("update rule failed", e);
                }
                break;
            }
            default: {
                //not implements
            }
            break;
        }
        return true;
    }

    private boolean checkPermission(@NonNull String permPackage) {
        int callingUid = Binder.getCallingUid();
        String[] packagesForUid = mContext.getPackageManager().getPackagesForUid(callingUid);
        return packagesForUid != null && Arrays.asList(packagesForUid).contains(permPackage);
    }

    private void enforcePermission(@NonNull String[] permPackages, String message) throws RemoteException {
        for (String permPackage : permPackages) {
            if (checkPermission(permPackage)) {
                return;
            }
        }
        throw new RemoteException(message);
    }

    private void enforcePermission(String message) throws RemoteException {
        if (!checkPermission(BuildConfig.APPLICATION_ID)) {
            throw new RemoteException(message);
        }
    }

    @Override
    public boolean hasLight() {
        return true;
    }

    /**
     * Set edit mode
     *
     * @param enable enable or disable
     */
    @Override
    public void setEditMode(boolean enable) throws RemoteException {
        enforcePermission("set edit mode fail permission denied");
        if (!mStarted) return;
        mInEditMode = enable;
        notifyObserverEditModeChanged(enable);
    }

    /**
     * Check in edit mode
     *
     * @return enable or disable
     */
    @Override
    public boolean isInEditMode() {
        return mInEditMode;
    }

    /**
     * Register an observer to be notified when status changed.
     *
     * @param packageName package name
     * @param observer    client observer
     */
    @Override
    public void addObserver(String packageName, IObserver observer) throws RemoteException {
        enforcePermission(new String[]{packageName, BuildConfig.APPLICATION_ID}, "register observer fail permission denied");
        if (!mStarted) return;
        synchronized (mRemoteCallbackList) {
            mRemoteCallbackList.register(new ObserverProxy(packageName, observer));
        }
    }

    /**
     * Unregister an observer
     *
     * @param packageName package name
     * @param observer    client observer
     * @throws RemoteException nothing
     */
    @Override
    public void removeObserver(String packageName, IObserver observer) throws RemoteException {
        enforcePermission(new String[]{packageName, BuildConfig.APPLICATION_ID}, "unregister observer fail permission denied");
        if (!mStarted) return;
        synchronized (mRemoteCallbackList) {
            mRemoteCallbackList.unregister(new ObserverProxy(packageName, observer));
        }
    }

    /**
     * Get all packages rules
     *
     * @return packages rules
     */
    @Override
    public AppRules getAllRules() throws RemoteException {
        enforcePermission("get all rules fail permission denied");
        if (!mStarted) return new AppRules();
        return mAppRulesCache;
    }

    /**
     * Get rules by package name
     *
     * @param packageName package name of the rule
     * @return rules
     */
    @Override
    public ActRules getRules(String packageName) throws RemoteException {
        enforcePermission(new String[]{packageName, BuildConfig.APPLICATION_ID}, "get rules fail permission denied");
        if (!mStarted) return new ActRules();
        return mAppRulesCache.containsKey(packageName) ? mAppRulesCache.get(packageName) : new ActRules();
    }

    /**
     * Write rule
     *
     * @param packageName package name of the rule
     * @param viewRule    rule object
     * @param snapshot    snapshot image of the view
     */
    @Override
    public boolean writeRule(String packageName, ViewRule viewRule, Bitmap snapshot) throws RemoteException {
        enforcePermission(new String[]{packageName, BuildConfig.APPLICATION_ID}, "write rule fail permission denied");
        if (!mStarted) return false;
        try {
            ActRules actRules = mAppRulesCache.get(packageName);
            if (actRules == null) {
                mAppRulesCache.put(packageName, actRules = new ActRules());
            }
            List<ViewRule> viewRules = actRules.get(viewRule.activityClass);
            if (viewRules == null) {
                actRules.put(viewRule.activityClass, viewRules = new ArrayList<>());
            }
            viewRules.add(viewRule);
            mHandle.obtainMessage(WRITE_RULE, new Object[]{actRules, packageName, viewRule, snapshot}).sendToTarget();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update rule of package
     *
     * @param packageName package name of the rule
     * @param viewRule    rule object
     * @return success or fail
     */
    @Override
    public boolean updateRule(String packageName, ViewRule viewRule) throws RemoteException {
        enforcePermission("update rule fail permission denied");
        if (!mStarted) return false;
        try {
            ActRules actRules = mAppRulesCache.get(packageName);
            if (actRules == null) {
                mAppRulesCache.put(packageName, actRules = new ActRules());
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
            mHandle.obtainMessage(UPDATE_RULE, new Object[]{actRules, packageName}).sendToTarget();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete the single rule of package
     *
     * @param packageName package name of the rule
     * @param viewRule    rule object
     * @return success or fail
     */
    @Override
    public boolean deleteRule(String packageName, ViewRule viewRule) throws RemoteException {
        enforcePermission("delete rule fail permission denied");
        if (!mStarted) return false;
        try {
            ActRules actRules = Preconditions.checkNotNull(mAppRulesCache.get(packageName), "not found this rule can't delete.");
            List<ViewRule> viewRules = Preconditions.checkNotNull(actRules.get(viewRule.activityClass), "not found this rule can't delete.");
            boolean removed = viewRules.remove(viewRule);
            if (removed) {
                if (viewRules.isEmpty()) {
                    actRules.remove(viewRule.activityClass);
                    if (actRules.isEmpty()) {
                        mAppRulesCache.remove(packageName);
                    }
                }
                mHandle.obtainMessage(DELETE_RULE, new Object[]{actRules, packageName, viewRule}).sendToTarget();
            }
            return removed;
        } catch (Exception e) {
            mLogger.w("delete rule failed", e);
            return false;
        }
    }

    /**
     * Delete all rules of package
     *
     * @param packageName package name of the rule
     * @return success or fail
     */
    @Override
    public boolean deleteRules(String packageName) throws RemoteException {
        enforcePermission("delete rules fail permission denied");
        if (!mStarted) return false;
        mLogger.d("delete rules pkg=" + packageName + " cache=" + mAppRulesCache);
        if (mAppRulesCache.containsKey(packageName)) {
            mAppRulesCache.remove(packageName);
            mHandle.obtainMessage(DELETE_RULES, packageName).sendToTarget();
            return true;
        }
        return false;
    }

    @Override
    public ParcelFileDescriptor openImageFileDescriptor(String filePath) throws RemoteException {
        enforcePermission("open fd fail permission denied");
        if (!filePath.startsWith(BASE_DIR) || !filePath.endsWith(IMAGE_FILE_SUFFIX))
            throw new RemoteException(String.format("unauthorized access %s", filePath));
        try {
            return ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            RemoteException remoteException = new RemoteException();
            remoteException.initCause(e);
            throw remoteException;
        }
    }

    private String saveBitmap(Bitmap bitmap, String dir) {
        try {
            File file = new File(dir, System.currentTimeMillis() + IMAGE_FILE_SUFFIX);
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
        synchronized (mRemoteCallbackList) {
            final int N = mRemoteCallbackList.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    ObserverProxy observerProxy = mRemoteCallbackList.getBroadcastItem(i);
                    if (TextUtils.equals(observerProxy.packageName, packageName) || TextUtils.equals(observerProxy.packageName, "*")) {
                        observerProxy.observer.onViewRuleChanged(packageName, actRules);
                    }
                } catch (Exception e) {
                    mLogger.w("notify rule changed fail", e);
                }
            }
            mRemoteCallbackList.finishBroadcast();
        }
    }

    private void notifyObserverEditModeChanged(boolean enable) {
        synchronized (mRemoteCallbackList) {
            final int N = mRemoteCallbackList.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    mRemoteCallbackList.getBroadcastItem(i).onEditModeChanged(enable);
                } catch (Exception e) {
                    mLogger.w("notify edit mode changed fail", e);
                }
            }
            mRemoteCallbackList.finishBroadcast();
        }
    }

    private String getBaseDir() throws FileNotFoundException {
        mLogger.d(BASE_DIR);
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
        public void onViewRuleChanged(String packageName, ActRules actRules) throws RemoteException {
            observer.onViewRuleChanged(packageName, actRules);
        }

        @Override
        public IBinder asBinder() {
            return observer.asBinder();
        }
    }

}
