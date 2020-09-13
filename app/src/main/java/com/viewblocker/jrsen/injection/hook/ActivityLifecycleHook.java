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
import java.util.List;
import java.util.Map;
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
    public void onPropertyChange(ActRules newActRules) {
        Set<Map.Entry<String, List<ViewRule>>> entrySet = newActRules.entrySet();
        for (Map.Entry<String, List<ViewRule>> entry : entrySet) {
            List<ViewRule> viewRules = actRules.get(entry.getKey());
            if (viewRules != null){
                viewRules.removeAll(entry.getValue());
            }
        }
        revokeRuleForActivity(actRules);
        actRules.clear();
        actRules.putAll(newActRules);
        applyRuleForActivity(actRules);
    }

    private void revokeRuleForActivity(ActRules actRules) {
        final int N = sActivities.size();
        for (int i = 0; i < N; i++) {
            Activity activity = sActivities.valueAt(i).get();
            if (activity != null && actRules.containsKey(activity.getComponentName().getClassName())) {
                List<ViewRule> viewRules = actRules.get(activity.getComponentName().getClassName());
                ViewController.revokeRuleBatch(activity, viewRules);
            }
        }
    }

    private void applyRuleForActivity(ActRules actRules) {
        final int N = sActivities.size();
        for (int i = 0; i < N; i++) {
            Activity activity = sActivities.valueAt(i).get();
            if (activity != null && actRules.containsKey(activity.getComponentName().getClassName())) {
                List<ViewRule> viewRules = actRules.get(activity.getComponentName().getClassName());
                ViewController.applyRuleBatch(activity, viewRules);
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
