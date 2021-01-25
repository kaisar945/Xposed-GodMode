package com.kaisar.xposed.godmode.fragment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SharedViewModel extends ViewModel {

    private final MutableLiveData<AppRules> mAppRules = new MutableLiveData<>();
    private final MutableLiveData<List<ViewRule>> mActRules = new MutableLiveData<>();

    public void loadAppRules() {
        mAppRules.setValue(GodModeManager.getDefault().getAllRules());
    }

    public LiveData<AppRules> getAppRules() {
        return mAppRules;
    }

    public LiveData<List<ViewRule>> getActRules() {
        return mActRules;
    }

    public void notifyActRulesChanged() {
        mActRules.postValue(mActRules.getValue());
    }

    public void selectActRules(String packageName) {
        AppRules appRules = mAppRules.getValue();
        ActRules actRules = appRules.get(packageName);
        ArrayList<ViewRule> viewRules = new ArrayList<>();
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
        mActRules.setValue(viewRules);
    }

    public boolean deleteAppRules(String packageName) {
        boolean ok = GodModeManager.getDefault().deleteRules(packageName);
        if (ok) {
            AppRules appRules = mAppRules.getValue();
            if (appRules.containsKey(packageName)) {
                appRules.remove(packageName);
                mAppRules.setValue(appRules);
                return true;
            }
        }
        return false;
    }

    public void updateRule(ViewRule rule) {
        if (GodModeManager.getDefault().updateRule(rule.packageName, rule)) {
            mActRules.setValue(mActRules.getValue());
        }
    }

    public void deleteRule(ViewRule rule) {
        if (GodModeManager.getDefault().deleteRule(rule.packageName, rule)) {
            AppRules appRules = mAppRules.getValue();
            ActRules actRules = appRules.get(rule.packageName);
            List<ViewRule> viewRules = actRules.get(rule.activityClass);
            if (viewRules.remove(rule)) {
                mActRules.setValue(viewRules);
            }
        }
    }

}
