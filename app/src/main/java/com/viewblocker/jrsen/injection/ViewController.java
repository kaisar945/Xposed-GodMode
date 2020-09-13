package com.viewblocker.jrsen.injection;

import android.app.Activity;
import android.util.Pair;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import static com.viewblocker.jrsen.BlockerApplication.TAG;

/**
 * Created by jrsen on 17-10-15.
 */

public final class ViewController {

    private static SparseArray<Pair<SoftReference<View>, ViewProperty>> blockedViewCache = new SparseArray<>();

    public static void applyRuleBatch(Activity activity, List<ViewRule> rules) {
        for (ViewRule rule : new ArrayList<>(rules)) {
            try {
                Logger.d(TAG, "apply rule:" + rule.toString());
                Pair<SoftReference<View>, ViewProperty> viewInfo = blockedViewCache.get(rule.hashCode());
                View view = viewInfo != null ? viewInfo.first.get() : null;
                if (view == null || ViewHelper.getAttachedActivityFromView(view) != activity) {
                    blockedViewCache.remove(rule.hashCode());
                    view = ViewHelper.findViewBestMatch(activity, rule);
                    Preconditions.checkNotNull(view, "apply rule fail can't found view by rule ");
                }
                boolean blocked = applyRule(view, rule);
                if (blocked) {
                    Logger.i(TAG, String.format("###block view success [Act]:%s  [View]:%s", activity, view));
                } else {
                    Logger.i(TAG, "###block view skipped this view already be blocked");
                }
            } catch (NullPointerException e) {
                Logger.w(TAG, String.format("###block view fail [Act]:%s  [View]:%s [Reason]:%s", activity, null, e.getMessage()));
            }
        }
    }

    public static boolean applyRule(View v, ViewRule viewRule) {
        Pair<SoftReference<View>, ViewProperty> viewInfo = blockedViewCache.get(viewRule.hashCode());
        View blockedView = viewInfo != null ? viewInfo.first.get() : null;
        if (blockedView == v && v.getVisibility() == viewRule.visibility) {
            return false;
        }
        ViewProperty viewProperty = blockedView == v ? viewInfo.second : ViewProperty.create(v);
        v.setAlpha(0f);
        v.setClickable(false);
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) {
            switch (viewRule.visibility) {
                case View.GONE:
                    lp.width = 0;
                    lp.height = 0;
                    break;
                case View.INVISIBLE:
                    lp.width = viewProperty.layout_params_width;
                    lp.height = viewProperty.layout_params_height;
                    break;
            }
        }
        v.setVisibility(viewRule.visibility);
        blockedViewCache.put(viewRule.hashCode(), Pair.create(new SoftReference<>(v), viewProperty));
        return true;
    }

    public static void revokeRuleBatch(Activity activity, List<ViewRule> rules) {
        for (ViewRule rule : new ArrayList<>(rules)) {
            try {
                Logger.d(TAG, "revoke rule:" + rule.toString());
                Pair<SoftReference<View>, ViewProperty> viewInfo = blockedViewCache.get(rule.hashCode());
                View view = viewInfo != null ? viewInfo.first.get() : null;
                if (view == null) {
                    blockedViewCache.remove(rule.hashCode());
                    view = ViewHelper.findViewBestMatch(activity, rule);
                    Preconditions.checkNotNull(view, "revoke rule fail can't found block view");
                }
                revokeRule(view, rule);
                Logger.i(TAG, String.format("###revoke rule success [Act]:%s  [View]:%s", activity, view));
            } catch (NullPointerException e) {
                Logger.w(TAG, String.format("###revoke rule fail [Act]:%s  [View]:%s [Reason]:%s", activity, null, e.getMessage()));
            }
        }
    }

    public static void revokeRule(View v, ViewRule viewRule) {
        Pair<SoftReference<View>, ViewProperty> viewInfo = blockedViewCache.get(viewRule.hashCode());
        if (viewInfo != null && viewInfo.first.get() == v) {
            ViewProperty viewProperty = viewInfo.second;
            v.setAlpha(viewProperty.alpha);
            v.setClickable(viewProperty.clickable);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp != null) {
                lp.width = viewProperty.layout_params_width;
                lp.height = viewProperty.layout_params_height;
            }
            v.setVisibility(viewRule.visibility);
            blockedViewCache.remove(viewRule.hashCode());
        } else {
            // cache missing why?
            v.setAlpha(1f);
            v.setVisibility(viewRule.visibility);
        }
    }

    private static final class ViewProperty {

        final float alpha;
        final boolean clickable;
        final int layout_params_width;
        final int layout_params_height;

        public ViewProperty(float alpha, boolean clickable, int layout_params_width, int layout_params_height) {
            this.alpha = alpha;
            this.clickable = clickable;
            this.layout_params_width = layout_params_width;
            this.layout_params_height = layout_params_height;
        }

        public static ViewProperty create(View view) {
            float alpha = view.getAlpha();
            boolean clickable = view.isClickable();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            int width = layoutParams != null ? layoutParams.width : 0;
            int height = layoutParams != null ? layoutParams.height : 1;
            return new ViewProperty(alpha, clickable, width, height);
        }
    }

}
