package com.kaisar.xposed.godmode.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kaisar.xposed.godmode.CrashHandler;
import com.kaisar.xposed.godmode.GodModeApplication;
import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.repository.LocalRepository;
import com.kaisar.xposed.godmode.repository.RemoteRepository;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Callback;

public class SharedViewModel extends ViewModel {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public final MutableLiveData<Integer> title = new MutableLiveData<>();
    public final MutableLiveData<AppRules> appRules = new MutableLiveData<>();
    public final MutableLiveData<String> selectedPackage = new MutableLiveData<>();
    public final MutableLiveData<List<ViewRule>> actRules = new MutableLiveData<>();

    public SharedViewModel() {
        LocalRepository.addObserver("*", new IObserver.Stub() {
            @Override
            public void onEditModeChanged(boolean enable) {
            }

            @Override
            public void onViewRuleChanged(String packageName, ActRules actRules) {
                appRules.postValue(LocalRepository.reloadAllAppRules());
                if (TextUtils.equals(packageName, selectedPackage.getValue())) {
                    selectedPackage.postValue(packageName);
                }
            }
        });
    }

    public void loadAppRules() {
        executor.execute(() -> appRules.postValue(LocalRepository.reloadAllAppRules()));
    }

    public void updateTitle(@StringRes int titleId) {
        title.setValue(titleId);
    }

    public void getGroupInfo(Callback<Map<String, String>[]> cb) {
        RemoteRepository.fetchGroupInfo(cb);
    }

    public void updateSelectedPackage(String packageName) {
        selectedPackage.postValue(packageName);
    }

    public void updateViewRuleList(String packageName) {
        ArrayList<ViewRule> viewRules = new ArrayList<>();
        AppRules appRules = this.appRules.getValue();
        if (appRules.containsKey(packageName)) {
            ActRules actRules = appRules.get(packageName);
            if (!actRules.isEmpty()) {
                for (List<ViewRule> values : actRules.values()) {
                    viewRules.addAll(values);
                }
                //Sort with generate timestamp
                Collections.sort(viewRules, Collections.reverseOrder(new Comparator<ViewRule>() {
                    @Override
                    public int compare(ViewRule o1, ViewRule o2) {
                        return Long.compare(o1.timestamp, o2.timestamp);
                    }
                }));
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

    public interface ImportCallback {
        void onSuccess();

        void onFailure(Throwable t);
    }

    public void importExternalRules(Context context, Uri uri, ImportCallback callback) {
        Handler handler = new Handler();
        executor.execute(() -> {
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                File file = new File(context.getCacheDir(), "app.gm");
                try {
                    if (FileUtils.copy(in, file)) {
                        if (LocalRepository.importRules(file.getPath())) {
                            handler.post(callback::onSuccess);
                        } else {
                            handler.post(() -> callback.onFailure(new Exception("import fail")));
                        }
                    } else {
                        handler.post(() -> callback.onFailure(new Exception("copy file error")));
                    }
                } finally {
                    FileUtils.delete(file);
                }
            } catch (Exception e) {
                handler.post(() -> callback.onFailure(e));
            }
        });
    }

}
