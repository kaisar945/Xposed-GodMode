package com.kaisar.xposed.godmode.injection.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import java.util.Iterator;
import java.util.List;

public class ActivityUtils {
    public static boolean isTopActivity(String cmdName, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(Integer.MAX_VALUE);
        String cmpNameTemp = null;
        if (null != runningTaskInfo) {
            cmpNameTemp = (runningTaskInfo.get(0).topActivity).getClassName();
        }
        if (null == cmpNameTemp) {
            return false;
        }
        return cmpNameTemp.equals(cmdName);
    }

    public static Activity getTopActivity(Context context, List<Activity> activities) {
        if (activities.size() == 0) {
            return null;
        }
        Iterator<Activity> iterator = activities.iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (activity.isFinishing()) {
                iterator.remove();
            } else {
                if (isTopActivity(activity.getClass().getName(), context)) {
                    return activity;
                }
            }
        }
        return null;
    }
}
