package com.viewblocker.jrsen.injection.hook;

import android.app.Activity;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.injection.ViewController;
import com.viewblocker.jrsen.injection.util.Property;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Created by jrsen on 17-10-15.
 */

public final class ActivityLifecycleHook extends XC_MethodHook implements Property.OnPropertyChangeListener<ActRules>, android.view.ViewTreeObserver.OnGlobalLayoutListener, ViewGroup.OnHierarchyChangeListener {

    private static SparseArray<SoftReference<Activity>> sActivities = new SparseArray<>();

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);
        Activity activity = (Activity) param.thisObject;
        String methodName = param.method.getName();
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        /*!!!这里有坑不要hook onCreate和onResume 因为getDecorView会执行installContentView的操作
         所以在Activity的子类中有可能去requestFeature会导致异常所以尽量找一个很靠后的生命周期函数*/
        if ("onPostResume".equals(methodName)) {
            sActivities.put(activity.hashCode(), new SoftReference<>(activity));
            decorView.setOnHierarchyChangeListener(this);
            decorView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        } else if ("onDestroy".equals(methodName)) {
            decorView.setOnHierarchyChangeListener(null);
            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            sActivities.remove(activity.hashCode());
        }
    }

    private ActRules actRules = new ActRules();

    @Override
    public void onPropertyChange(ActRules actRules) {
        ActRules legacyActRules = new ActRules();
        Set<String> keySet = this.actRules.keySet();
        for (String key : keySet) {
            if (actRules.containsKey(key)) {
                List<ViewRule> legacyRuleList = new ArrayList<>(this.actRules.get(key));
                legacyRuleList.removeAll(actRules.get(key));
                legacyActRules.put(key, legacyRuleList);
            } else {
                legacyActRules.put(key, this.actRules.get(key));
            }
        }
        this.actRules = actRules;
        revokeRuleForActivity(legacyActRules);
        applyRuleForActivity(actRules);
    }

    private void revokeRuleForActivity(ActRules legacyActRules) {
        if (legacyActRules == null || legacyActRules.isEmpty()) {
            return;
        }
        for (int i = 0; i < sActivities.size(); i++) {
            SoftReference<Activity> activitySoftReference = sActivities.valueAt(i);
            Activity activity = activitySoftReference.get();
            if (activity != null) {
                if (legacyActRules.containsKey(activity.getComponentName().getClassName())) {
                    ViewController.revokeRule(activity, legacyActRules.getRuleList(activity));
                }
            } else {
                sActivities.removeAt(i--);
            }
        }
    }

    private void applyRuleForActivity(ActRules actRules) {
        if (actRules == null || actRules.isEmpty()) {
            return;
        }
        for (int i = 0; i < sActivities.size(); i++) {
            SoftReference<Activity> activitySoftReference = sActivities.valueAt(i);
            Activity activity = activitySoftReference.get();
            if (activity != null) {
                if (actRules.containsKey(activity.getComponentName().getClassName())) {
                    ViewController.applyRule(activity, actRules.getRuleList(activity));
                }
            } else {
                sActivities.removeAt(i--);
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        applyRuleForActivity(actRules);
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        applyRuleForActivity(actRules);
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        applyRuleForActivity(actRules);
    }

}
