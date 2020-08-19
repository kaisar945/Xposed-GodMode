package com.viewblocker.jrsen.injection;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

import static com.viewblocker.jrsen.BlockerApplication.TAG;

/**
 * Created by jrsen on 17-10-15.
 */

public final class ViewController {

    private static final String CLICKABLE = "clickable";
    private static final String LAYOUT_PARAMS_WIDTH = "layout_params_width";
    private static final String LAYOUT_PARAMS_HEIGHT = "layout_params_height";

    public static void applyRules(Activity activity, List<ViewRule> rules) {
        for (ViewRule rule : new ArrayList<>(rules)) {
            try {
                View view = Preconditions.checkNotNull(ViewHelper.findViewBestMatch(activity, rule), "can't found block view apply rule failed");
                Logger.d(TAG, String.format("###block### [Act]:%s  [View]:%s", activity, view));
                applyRules(view, rule);
            } catch (NullPointerException ignored) {
//                ignored.printStackTrace();
            }
        }
    }

    public static void applyRules(View v, ViewRule viewRule) {
        saveViewPropertyIfNeeded(v);
        v.setClickable(false);
        v.setAlpha(0f);
        v.setVisibility(viewRule.visibility);
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (viewRule.visibility == View.GONE && lp != null) {
            lp.width = 0;
            lp.height = 0;
        } else if (viewRule.visibility == View.INVISIBLE && lp != null) {
            lp.width = (int) XposedHelpers.getAdditionalInstanceField(v, LAYOUT_PARAMS_WIDTH);
            lp.height = (int) XposedHelpers.getAdditionalInstanceField(v, LAYOUT_PARAMS_HEIGHT);
        }
    }

    private static void saveViewPropertyIfNeeded(View v) {
        if (XposedHelpers.getAdditionalInstanceField(v, CLICKABLE) == null) {
            XposedHelpers.setAdditionalInstanceField(v, CLICKABLE, v.isClickable());
        }
        if (XposedHelpers.getAdditionalInstanceField(v, LAYOUT_PARAMS_WIDTH) == null && XposedHelpers.getAdditionalInstanceField(v, LAYOUT_PARAMS_HEIGHT) == null) {
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            if (layoutParams != null) {
                XposedHelpers.setAdditionalInstanceField(v, LAYOUT_PARAMS_WIDTH, layoutParams.width);
                XposedHelpers.setAdditionalInstanceField(v, LAYOUT_PARAMS_HEIGHT, layoutParams.height);
            }
        }
    }

    public static void revokeRule(Activity activity, List<ViewRule> rules) {
        for (ViewRule rule : new ArrayList<>(rules)) {
            try {
                View view = Preconditions.checkNotNull(ViewHelper.findViewBestMatch(activity, rule), "can't found block view revoke rule failed");
                Logger.d(TAG, String.format("###revoke block### [Act]:%s  [View]:%s", activity, view));
                //revoke block view
                rule.visibility = View.VISIBLE;
                ViewController.revokeRule(view, rule);
            } catch (NullPointerException ignored) {
//                ignored.printStackTrace();
            }
        }
    }

    public static void revokeRule(View v, ViewRule viewRule) {
        restoreViewPropertyIfNeeded(v);
        v.setAlpha(1f);
        v.setVisibility(viewRule.visibility);
    }

    private static void restoreViewPropertyIfNeeded(View v) {
        Object clickable = XposedHelpers.removeAdditionalInstanceField(v, CLICKABLE);
        if (clickable != null) {
            v.setClickable((Boolean) clickable);
        }
        Object width = XposedHelpers.removeAdditionalInstanceField(v, LAYOUT_PARAMS_WIDTH);
        Object height = XposedHelpers.removeAdditionalInstanceField(v, LAYOUT_PARAMS_HEIGHT);
        ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        if (width != null && height != null && layoutParams != null) {
            layoutParams.width = (int) width;
            layoutParams.height = (int) height;
            v.requestLayout();
        }
    }

}
