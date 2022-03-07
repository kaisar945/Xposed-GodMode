package com.kaisar.xposed.godmode.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.bean.GroupInfo;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.repository.LocalRepository;
import com.kaisar.xposed.godmode.repository.RemoteRepository;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.BackupUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Callback;

public class SharedViewModel extends ViewModel {

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    public final MutableLiveData<AppRules> appRules = new MutableLiveData<>();
    public final MutableLiveData<List<ViewRule>> actRules = new MutableLiveData<>();
    public final MutableLiveData<String> selectedPackage = new MutableLiveData<>();

    public SharedViewModel() {
        LocalRepository.addObserver("*", new IObserver.Stub() {
            @Override
            public void onEditModeChanged(boolean enable) {
            }

            @Override
            public void onViewRuleChanged(String packageName, ActRules actRules) {
                appRules.postValue(LocalRepository.loadAppRules());
                if (TextUtils.equals(packageName, selectedPackage.getValue())) {
                    selectedPackage.postValue(packageName);
                }
            }
        });
    }

    public void loadAppRules() {
        mExecutor.execute(() -> appRules.postValue(LocalRepository.loadAppRules()));
    }

    public void getGroupInfo(Callback<List<GroupInfo>> cb) {
        RemoteRepository.fetchGroupInfo(cb);
    }

    public void updateSelectedPackage(String packageName) {
        selectedPackage.postValue(packageName);
    }

    public void updateViewRuleList(String packageName) {
        ArrayList<ViewRule> viewRules = new ArrayList<>();
        AppRules appRules = this.appRules.getValue();
        if (appRules != null && appRules.containsKey(packageName)) {
            ActRules actRules = appRules.get(packageName);
            if (actRules != null && !actRules.isEmpty()) {
                for (List<ViewRule> values : actRules.values()) {
                    viewRules.addAll(values);
                }
                //Sort with generate timestamp
                Collections.sort(viewRules, (o1, o2) -> (int) (o1.timestamp - o2.timestamp));
            }
        }
        actRules.setValue(viewRules);
    }

    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public boolean deleteAppRules(String packageName) {
        return GodModeManager.getDefault().deleteRules(packageName);
    }

    public boolean updateRule(ViewRule rule) {
        return GodModeManager.getDefault().updateRule(rule.packageName, rule);
    }

    public boolean deleteRule(ViewRule rule) {
        return GodModeManager.getDefault().deleteRule(rule.packageName, rule);
    }

    public void setIconHidden(Context context, boolean hidden) {
        PackageManager pm = context.getPackageManager();
        ComponentName cmp = new ComponentName(context.getPackageName(), "com.kaisar.xposed.godmode.SettingsAliasActivity");
        pm.setComponentEnabledSetting(cmp, hidden ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public boolean isIconHidden(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName cmp = new ComponentName(context.getPackageName(), "com.kaisar.xposed.godmode.SettingsAliasActivity");
        return pm.getComponentEnabledSetting(cmp) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    public interface ResultCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    public void restoreRules(Uri uri, ResultCallback callback) {
        mExecutor.execute(() -> {
            try {
                BackupUtils.restoreRules(uri);
                mMainHandler.post(callback::onSuccess);
            } catch (BackupUtils.RestoreException e) {
                mMainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public void backupRules(Uri uri, String packageName, List<ViewRule> viewRules, ResultCallback callback) {
        mExecutor.execute(() -> {
            try {
                BackupUtils.backupRules(uri, packageName, viewRules);
                mMainHandler.post(callback::onSuccess);
            } catch (BackupUtils.BackupException e) {
                mMainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

}
