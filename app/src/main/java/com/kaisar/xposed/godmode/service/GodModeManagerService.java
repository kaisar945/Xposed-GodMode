package com.kaisar.xposed.godmode.service;


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

import com.google.gson.Gson;
import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.IGodModeManager;
import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.Preconditions;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;
import static com.kaisar.xposed.godmode.injection.util.FileUtils.S_IRWXG;
import static com.kaisar.xposed.godmode.injection.util.FileUtils.S_IRWXO;
import static com.kaisar.xposed.godmode.injection.util.FileUtils.S_IRWXU;


/**
 * Created by jrsen on 17-10-15.
 * 上帝模式核心管理服务所有跨进程通讯均通过此服务
 * 该服务通过Xposed注入到SystemServer进程作为一个系统服务
 * Client端可以使用{@link GodModeManager#getDefault()}使用该服务提供的接口
 */

public final class GodModeManagerService extends IGodModeManager.Stub implements Handler.Callback {

    // /data/godmode
    private final static String BASE_DIR = String.format("%s/%s", Environment.getDataDirectory().getAbsolutePath(), "godmode");
    // /data/godmode/conf
    private final static String CONFIG_FILE_NAME = "conf";
    // /data/godmode/package/package.rule
    private final static String RULE_FILE_SUFFIX = ".rule";

    private static final int WRITE_RULE = 0x00002;
    private static final int DELETE_RULE = 0x00004;
    private static final int DELETE_RULES = 0x00008;
    private static final int UPDATE_RULE = 0x000016;

    private final RemoteCallbackList<ObserverProxy> mRemoteCallbackList = new RemoteCallbackList<>();
    private final HashMap<String, ActRules> mRuleCache = new HashMap<>();
    private final Context mContext;
    private final Handler mHandle;
    private boolean mInEditMode;
    private boolean mStarted;

    public GodModeManagerService(Context context) {
        mContext = context;
        HandlerThread workThread = new HandlerThread("work-thread");
        workThread.start();
        mHandle = new Handler(workThread.getLooper(), this);
        try {
            loadPreferenceData();
            mStarted = true;
        } catch (Exception e) {
            mStarted = false;
            Logger.e(TAG, "loadPreferenceData failed", e);
        }
    }

    private void loadPreferenceData() throws IOException {
        File dataDir = new File(getBaseDir());
        File[] packageDirs = dataDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (packageDirs != null && packageDirs.length > 0) {
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
            mRuleCache.putAll(appRules);
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
                //not implements
            }
            break;
        }
        return true;
    }

    private boolean checkPermission(String permPackage) {
        int callingUid = Binder.getCallingUid();
        String[] packagesForUid = mContext.getPackageManager().getPackagesForUid(callingUid);
        return packagesForUid != null && Arrays.asList(packagesForUid).contains(permPackage);
    }

    private void enforcePermission(String permPackage, String message) throws RemoteException {
        if (!checkPermission(permPackage)) {
            throw new RemoteException(message);
        }
    }

    /**
     * Set edit mode
     *
     * @param enable enable or disable
     */
    @Override
    public void setEditMode(boolean enable) throws RemoteException {
        enforcePermission(BuildConfig.APPLICATION_ID, "set edit mode fail permission denied");
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
     * @param packageName client package name
     * @param observer    client observer
     */
    @Override
    public void addObserver(String packageName, IObserver observer) throws RemoteException {
        if (!checkPermission(packageName) && !checkPermission(BuildConfig.APPLICATION_ID)) {
            throw new RemoteException("register observer fail permission denied");
        }
        if (!mStarted) return;
        mRemoteCallbackList.register(new ObserverProxy(packageName, observer));
    }

    /**
     * Get all packages rules
     *
     * @return packages rules
     */
    @Override
    public Map<String, ActRules> getAllRules() throws RemoteException {
        enforcePermission(BuildConfig.APPLICATION_ID, "get all rules fail permission denied");
        if (!mStarted) return Collections.emptyMap();
        return mRuleCache;
    }

    /**
     * Get rules by package name
     *
     * @param packageName package name of the rule
     * @return rules
     */
    @Override
    public ActRules getRules(String packageName) throws RemoteException {
        if (!checkPermission(packageName) && !checkPermission(BuildConfig.APPLICATION_ID)) {
            throw new RemoteException("get rules fail permission denied");
        }
        if (!mStarted) return new ActRules();
        return mRuleCache.containsKey(packageName) ? mRuleCache.get(packageName) : new ActRules();
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
        if (!checkPermission(packageName) && !checkPermission(BuildConfig.APPLICATION_ID)) {
            throw new RemoteException("write rule fail permission denied");
        }
        if (!mStarted) return false;
        try {
            ActRules actRules = mRuleCache.get(packageName);
            if (actRules == null) {
                mRuleCache.put(packageName, actRules = new ActRules());
            }
            List<ViewRule> viewRules = actRules.get(viewRule.activityClass);
            if (viewRules == null) {
                actRules.put(viewRule.activityClass, viewRules = new ArrayList<>());
            }
            viewRules.add(viewRule);
            mHandle.obtainMessage(WRITE_RULE, new Object[]{actRules, packageName, viewRule, snapshot}).sendToTarget();
            notifyObserverRuleChanged(packageName, actRules);
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
        enforcePermission(BuildConfig.APPLICATION_ID, "update rule fail permission denied");
        if (!mStarted) return false;
        try {
            ActRules actRules = mRuleCache.get(packageName);
            if (actRules == null) {
                mRuleCache.put(packageName, actRules = new ActRules());
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
            notifyObserverRuleChanged(packageName, actRules);
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
        enforcePermission(BuildConfig.APPLICATION_ID, "delete rule fail permission denied");
        if (!mStarted) return false;
        try {
            ActRules actRules = Preconditions.checkNotNull(mRuleCache.get(packageName), "not found this rule can't delete.");
            List<ViewRule> viewRules = Preconditions.checkNotNull(actRules.get(viewRule.activityClass), "not found this rule can't delete.");
            boolean removed = viewRules.remove(viewRule);
            if (removed) {
                mHandle.obtainMessage(DELETE_RULE, new Object[]{actRules, packageName, viewRule}).sendToTarget();
                notifyObserverRuleChanged(packageName, actRules);
            }
            return removed;
        } catch (Exception e) {
            e.printStackTrace();
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
        enforcePermission(BuildConfig.APPLICATION_ID, "delete rules fail permission denied");
        if (!mStarted) return false;
        try {
            mRuleCache.remove(packageName);
            mHandle.obtainMessage(DELETE_RULES, packageName).sendToTarget();
            notifyObserverRuleChanged(packageName, new ActRules());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(String filePath, int mode) throws RemoteException {
        enforcePermission(BuildConfig.APPLICATION_ID, "open file fail permission denied");
        try {
            return ParcelFileDescriptor.open(new File(filePath), mode);
        } catch (FileNotFoundException e) {
            RemoteException remoteException = new RemoteException();
            remoteException.initCause(e);
            throw remoteException;
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
        final int N = mRemoteCallbackList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                ObserverProxy observerProxy = mRemoteCallbackList.getBroadcastItem(i);
                if (TextUtils.equals(observerProxy.packageName, packageName)) {
                    observerProxy.observer.onViewRuleChanged(actRules);
                }
            } catch (RemoteException ignore) {
//                ignore.printStackTrace();
            }
        }
        mRemoteCallbackList.finishBroadcast();
    }

    private void notifyObserverEditModeChanged(boolean enable) {
        final int N = mRemoteCallbackList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mRemoteCallbackList.getBroadcastItem(i).onEditModeChanged(enable);
            } catch (RemoteException ignore) {
//                ignore.printStackTrace();
            }
        }
        mRemoteCallbackList.finishBroadcast();
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
