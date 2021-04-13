package com.kaisar.xposed.godmode.model;

import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kaisar.xposed.godmode.CrashHandler;
import com.kaisar.xposed.godmode.GodModeApplication;
import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.repository.LocalRepository;
import com.kaisar.xposed.godmode.repository.RemoteRepository;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import retrofit2.Callback;

public class SharedViewModel extends ViewModel {

    public final MutableLiveData<Integer> title = new MutableLiveData<>();
    public final MutableLiveData<String> crash = new MutableLiveData<>();
    public final MutableLiveData<AppRules> appRules = new MutableLiveData<>();
    public final MutableLiveData<String> selectedPackage = new MutableLiveData<>();
    public final MutableLiveData<List<ViewRule>> actRules = new MutableLiveData<>();

    public SharedViewModel() {
        GodModeManager.getDefault().addObserver("*", new IObserver.Stub() {
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
        String crashMessage = CrashHandler.detectCrash(GodModeApplication.getApplication());
        if (crashMessage != null) {
            crash.setValue(crashMessage);
        }
    }

    public void reloadAppRules() {
        AppRules appRules = GodModeManager.getDefault().getAllRules();
        if (isMainThread()) {
            this.appRules.setValue(appRules);
        } else {
            this.appRules.postValue(appRules);
        }
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

}
