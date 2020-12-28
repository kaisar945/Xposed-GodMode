package com.kaisar.xposed.godmode.injection;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.Preconditions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XposedHelpers;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

/**
 * Created by jrsen on 17-10-13.
 */

public final class ViewHelper {

    public static final String TAG_GM_CMP = "gm_cmp";

    public static View findViewBestMatch(Activity activity, ViewRule rule) {
        // if the rule version and the application version are the same, use strict mode.
        boolean strictMode = false;
        try {
            ClassLoader cl = activity.getClassLoader();
            Class<?> BuildConfigClass = cl.loadClass(activity.getPackageName() + ".BuildConfig");
            int versionCode = BuildConfigClass.getField("VERSION_CODE").getInt(null);
            strictMode = versionCode == rule.matchVersionCode;
        } catch (Exception ignore) {
            try {
                PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                strictMode = packageInfo.versionCode == rule.matchVersionCode;
            } catch (PackageManager.NameNotFoundException e) {
                Logger.w(TAG, "See what happened!", e);
            }
        }
        if (!TextUtils.isEmpty(rule.description)) {
            Logger.i(TAG, String.format("strict mode %b, match view by description", strictMode));
            View view = matchView(findViewByDescription(activity.getWindow().getDecorView(), rule.description), rule, strictMode);
            if (view != null) {
                return view;
            }
        }
        if (!TextUtils.isEmpty(rule.text)) {
            Logger.i(TAG, String.format("strict mode %b, match view by text", strictMode));
            View view = matchView(findViewByText(activity.getWindow().getDecorView(), rule.text), rule, strictMode);
            if (view != null) {
                return view;
            }
        }
        if (!TextUtils.isEmpty(rule.resourceName)) {
            Logger.i(TAG, String.format("strict mode %b, match view by resource name", strictMode));
            View view = matchView(activity.findViewById(rule.getViewId(activity.getResources())), rule, strictMode);
            if (view != null) {
                return view;
            }
        }
        Logger.i(TAG, String.format("strict mode %b, match view by depth", strictMode));
        View view = matchView(findViewByDepth(activity, rule.depth), rule, strictMode);
        return view;
    }

    private static View matchView(View view, ViewRule rule, boolean strictMode) {
        try {
            Preconditions.checkNotNull(view, "view can't be null");
            Preconditions.checkNotNull(rule, "rule can't be null");
            String resourceName = null;
            try {
                resourceName = view.getResources().getResourceName(view.getId());
            } catch (Resources.NotFoundException ignore) {
            }
            String text = (view instanceof TextView) ? Preconditions.optionDefault(((TextView) view).getText(), "").toString() : "";
            String description = Preconditions.optionDefault(view.getContentDescription(), "").toString();
            String viewClass = view.getClass().getName();
            Logger.i(TAG, String.format("view res name:%s matched:%b", resourceName, TextUtils.equals(resourceName, rule.resourceName)));
            Logger.i(TAG, String.format("view text:%s matched:%b", text, TextUtils.equals(text, rule.text)));
            Logger.i(TAG, String.format("view description:%s matched:%b", description, TextUtils.equals(description, rule.description)));
            Logger.i(TAG, String.format("view class:%s matched:%b", viewClass, TextUtils.equals(viewClass, rule.viewClass)));
            if (strictMode) {
                return TextUtils.equals(resourceName, rule.resourceName)
                        && TextUtils.equals(text, rule.text)
                        && TextUtils.equals(description, rule.description)
                        && TextUtils.equals(viewClass, rule.viewClass) ? view : null;
            } else {
                return ((!TextUtils.isEmpty(rule.resourceName) && TextUtils.equals(resourceName, rule.resourceName))
                        || (!TextUtils.isEmpty(rule.text) && TextUtils.equals(text, rule.text))
                        || (!TextUtils.isEmpty(rule.description) && TextUtils.equals(description, rule.description))
                        || (!TextUtils.isEmpty(rule.viewClass) && TextUtils.equals(viewClass, rule.viewClass))) ? view : null;

            }
        } catch (Exception ignore) {
//            ignore.printStackTrace();
        }
        return null;
    }

    public static View findViewByText(View view, String text) {
        if (view instanceof TextView && TextUtils.equals(((TextView) view).getText(), text)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            final int N = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < N; i++) {
                View childView = findViewByText(((ViewGroup) view).getChildAt(i), text);
                if (childView != null) {
                    return childView;
                }
            }
        }
        return null;
    }

    public static View findViewByDescription(View view, String description) {
        if (TextUtils.equals(view.getContentDescription(), description)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            final int N = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < N; i++) {
                View childView = findViewByDescription(((ViewGroup) view).getChildAt(i), description);
                if (childView != null) {
                    return childView;
                }
            }
        }
        return null;
    }

    public static View findViewByDepth(Activity activity, int[] depths) {
        View view = activity.getWindow().getDecorView();
        for (int depth : depths) {
            view = view instanceof ViewGroup
                    ? ((ViewGroup) view).getChildAt(depth) : null;
            if (view == null) break;
        }
        return view;
    }

    public static View findTopParentViewByChildView(View v) {
        if (v.getParent() == null || !(v.getParent() instanceof ViewGroup)) {
            return v;
        } else {
            return findTopParentViewByChildView((View) v.getParent());
        }
    }

    public static Object findViewRootImplByChildView(ViewParent parent) {
        if (parent.getParent() == null) {
            return !(parent instanceof ViewGroup) ? parent : null;
        } else {
            return findViewRootImplByChildView(parent.getParent());
        }
    }

    public static int[] getViewHierarchyDepth(View view) {
        int[] depth = new int[0];
        ViewParent parent = view.getParent();
        while (parent instanceof ViewGroup) {
            int[] newDepth = new int[depth.length + 1];
            System.arraycopy(depth, 0, newDepth, 1, depth.length);
            newDepth[0] = ((ViewGroup) parent).indexOfChild(view);
            depth = newDepth;
            view = (View) parent;
            parent = parent.getParent();
        }
        return depth;
    }

    public static ViewRule makeRule(View v) throws PackageManager.NameNotFoundException {
        Activity activity = getAttachedActivityFromView(v);
        Objects.requireNonNull(activity, "Can't found attached activity");
        int[] out = new int[2];
        v.getLocationInWindow(out);
        int x = out[0];
        int y = out[1];
        int width = v.getWidth();
        int height = v.getHeight();

        int[] viewHierarchyDepth = getViewHierarchyDepth(v);
        String activityClassName = activity.getComponentName().getClassName();
        String viewClassName = v.getClass().getName();
        Context context = v.getContext();
        Resources res = context.getResources();
        String resourceName = null;
        try {
            resourceName = v.getId() != View.NO_ID ? res.getResourceName(v.getId()) : null;
        } catch (Resources.NotFoundException ignore) {
            //the resource id may be declared in the plugin apk
        }
        String text = (v instanceof TextView && !TextUtils.isEmpty(((TextView) v).getText())) ? ((TextView) v).getText().toString() : "";
        String description = (!TextUtils.isEmpty(v.getContentDescription())) ? v.getContentDescription().toString() : "";
        String alias = !TextUtils.isEmpty(text) ? text : description;
        String packageName = context.getPackageName();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        String label = packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
        String versionName = packageInfo.versionName;
        int versionCode = packageInfo.versionCode;
        return new ViewRule(label, packageName, versionName, versionCode, BuildConfig.VERSION_CODE, "", alias, x, y, width, height, viewHierarchyDepth, activityClassName, viewClassName, resourceName, text, description, View.INVISIBLE, System.currentTimeMillis());
    }

    public static Activity getAttachedActivityFromView(View view) {
        Activity activity = getActivityFromViewContext(view.getContext());
        if (activity != null) {
            return activity;
        } else {
            ViewParent parent = view.getParent();
            return parent instanceof ViewGroup ? getAttachedActivityFromView((View) parent) : null;
        }
    }

    private static Activity getActivityFromViewContext(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            //这是不直接getBaseContext方法获取 因为撒比微信有个PluginContextWrapper getBaseContext返回的是this导致栈溢出
            Context baseContext = ((ContextWrapper) context).getBaseContext();
            if (baseContext == context) {
                baseContext = (Context) XposedHelpers.getObjectField(context, "mBase");
            }
            return getActivityFromViewContext(baseContext);
        } else {
            return null;
        }
    }

    public static Bitmap snapshotView(View view) {
        boolean enable = view.isDrawingCacheEnabled();
        view.setDrawingCacheEnabled(true);
        Bitmap b = view.getDrawingCache();
        b = b == null ? snapshotViewCompat(view) : Bitmap.createBitmap(b);
        view.setDrawingCacheEnabled(enable);
        return b;
    }

    private static Bitmap snapshotViewCompat(View v) {
        //有些view宽高为0神奇!!!
        Bitmap b = Bitmap.createBitmap(Math.max(v.getWidth(), 1), Math.max(v.getHeight(), 1), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }

    public static Bitmap cloneViewAsBitmap(View view) {
        Bitmap bitmap = snapshotView(view);

        Paint paint = new Paint();
        paint.setAntiAlias(false);

        // Draw optical bounds
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);

        Canvas canvas = new Canvas(bitmap);
        drawRect(canvas, paint, 0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);

//        // Draw margins
//        {
//            paint.setColor(Color.argb(63, 255, 0, 255));
//            paint.setStyle(Paint.Style.FILL);
//
//            onDebugDrawMargins(canvas, paint);
//        }

        // Draw clip bounds
        paint.setColor(Color.rgb(63, 127, 255));
        paint.setStyle(Paint.Style.FILL);

        Context context = view.getContext();
        int lineLength = dipsToPixels(context, 8);
        int lineWidth = dipsToPixels(context, 1);
        drawRectCorners(canvas, 0, 0, canvas.getWidth(), canvas.getHeight(),
                paint, lineLength, lineWidth);
        return bitmap;
    }

    private static int dipsToPixels(Context context, int dips) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dips * scale + 0.5f);
    }

    private static void drawRect(Canvas canvas, Paint paint, int x1, int y1, int x2, int y2) {
        float[] debugLines = new float[16];

        debugLines[0] = x1;
        debugLines[1] = y1;
        debugLines[2] = x2;
        debugLines[3] = y1;

        debugLines[4] = x2;
        debugLines[5] = y1;
        debugLines[6] = x2;
        debugLines[7] = y2;

        debugLines[8] = x2;
        debugLines[9] = y2;
        debugLines[10] = x1;
        debugLines[11] = y2;

        debugLines[12] = x1;
        debugLines[13] = y2;
        debugLines[14] = x1;
        debugLines[15] = y1;

        canvas.drawLines(debugLines, paint);
    }

    private static void drawRectCorners(Canvas canvas, int x1, int y1, int x2, int y2, Paint paint,
                                        int lineLength, int lineWidth) {
        drawCorner(canvas, paint, x1, y1, lineLength, lineLength, lineWidth);
        drawCorner(canvas, paint, x1, y2, lineLength, -lineLength, lineWidth);
        drawCorner(canvas, paint, x2, y1, -lineLength, lineLength, lineWidth);
        drawCorner(canvas, paint, x2, y2, -lineLength, -lineLength, lineWidth);
    }

    private static void drawCorner(Canvas c, Paint paint, int x1, int y1, int dx, int dy, int lw) {
        fillRect(c, paint, x1, y1, x1 + dx, y1 + lw * sign(dy));
        fillRect(c, paint, x1, y1, x1 + lw * sign(dx), y1 + dy);
    }

    private static void fillRect(Canvas canvas, Paint paint, int x1, int y1, int x2, int y2) {
        if (x1 != x2 && y1 != y2) {
            if (x1 > x2) {
                int tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            if (y1 > y2) {
                int tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            canvas.drawRect(x1, y1, x2, y2, paint);
        }
    }

    private static int sign(int x) {
        return (x >= 0) ? 1 : -1;
    }

    public static List<WeakReference<View>> buildViewNodes(View view) {
        ArrayList<WeakReference<View>> views = new ArrayList<>();
        if (view.getVisibility() == View.VISIBLE && !TAG_GM_CMP.equals(view.getTag())) {
            views.add(new WeakReference<>(view));
            if (view instanceof ViewGroup) {
                final int N = ((ViewGroup) view).getChildCount();
                for (int i = 0; i < N; i++) {
                    View childView = ((ViewGroup) view).getChildAt(i);
                    views.addAll(buildViewNodes(childView));
                }
            }
        }
        return views;
    }

    public static Rect getLocationInWindow(View v) {
        int[] out = new int[2];
        v.getLocationInWindow(out);
        int l = out[0];
        int t = out[1];
        int r = l + v.getWidth();
        int b = t + v.getHeight();
        return new Rect(l, t, r, b);
    }

    public static Rect getLocationOnScreen(View v) {
        int[] out = new int[2];
        v.getLocationOnScreen(out);
        int l = out[0];
        int t = out[1];
        int r = l + v.getWidth();
        int b = t + v.getHeight();
        return new Rect(l, t, r, b);
    }

}
