package com.kaisar.xposed.godmode.fragment;

import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SharedViewModel extends ViewModel {

    private final Logger mLogger;
    private final MutableLiveData<AppRules> mAppRules = new MutableLiveData<>();
    private final MutableLiveData<String> mSelectedPackage = new MutableLiveData<>();
    private final MutableLiveData<List<ViewRule>> mActRules = new MutableLiveData<>();

    public SharedViewModel() {
        this.mLogger = Logger.getLogger("流程");
    }

    public void reloadAppRules() {
        AppRules appRules = GodModeManager.getDefault().getAllRules();
//        mLogger.d("重新加載應用規則列表:" + appRules.size());
        if (isMainThread()) {
            mAppRules.setValue(appRules);
        } else {
            mAppRules.postValue(appRules);
        }
    }

    public MutableLiveData<AppRules> getAppRules() {
        return mAppRules;
    }

    public MutableLiveData<List<ViewRule>> getActRules() {
        return mActRules;
    }

    public MutableLiveData<String> getSelectedPackage() {
        return mSelectedPackage;
    }

    public void updateSelectedPackage(String packageName) {
        mSelectedPackage.postValue(packageName);
    }

    public void updateViewRuleList(String packageName) {
        ArrayList<ViewRule> viewRules = new ArrayList<>();
        AppRules appRules = mAppRules.getValue();
//        mLogger.d("重新加載appRules列表:" + appRules.size());
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
//        mLogger.d("重新加載act列表:" + viewRules);
        mActRules.setValue(viewRules);
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
