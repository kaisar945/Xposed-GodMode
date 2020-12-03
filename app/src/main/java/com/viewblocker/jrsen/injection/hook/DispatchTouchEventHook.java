package com.viewblocker.jrsen.injection.hook;

import android.animation.Animator;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.Toast;

import com.viewblocker.jrsen.injection.BlockerInjector;
import com.viewblocker.jrsen.injection.ViewController;
import com.viewblocker.jrsen.injection.ViewHelper;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.injection.weiget.CancelView;
import com.viewblocker.jrsen.injection.weiget.MaskView;
import com.viewblocker.jrsen.injection.weiget.ParticleView;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static com.viewblocker.jrsen.BlockerApplication.TAG;
import static com.viewblocker.jrsen.injection.ViewHelper.TAG_GM_CMP;

/**
 * Created by jrsen on 17-12-6.
 */

public final class DispatchTouchEventHook extends XC_MethodHook {

    private float x, y;
    private Bitmap snapshot;
    private ViewRule viewRule;
    private MaskView maskView;
    private CancelView cancelView;
    private boolean hasBlockEvent;
    public static volatile boolean isDragging;

    private boolean isLongClick;
    private CheckForLongPress pendingCheckForLongPress;
    private Handler handler = new Handler(Looper.getMainLooper());

    private volatile boolean multiPointLock;

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        View view = (View) param.thisObject;
        MotionEvent event = (MotionEvent) param.args[0];

        if (BlockerInjector.switchProp.get() && !TAG_GM_CMP.equals(view.getTag())) {
            param.setResult(dispatchTouchEvent(view, event));
        }
    }

    private boolean dispatchTouchEvent(View v, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (multiPointLock) {
                Toast.makeText(v.getContext(), "不支持多点操作", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!isAttachedToActivity(v)) {
                if (!hasBlockEvent) {
                    Toast.makeText(v.getContext(), "该控件属于悬浮窗暂不支持编辑", Toast.LENGTH_SHORT).show();
                    hasBlockEvent = true;
                }
                return false;
            }
            isDragging = true;
            multiPointLock = true;//防止多个触点同时触发
            //防止列表控件拦截事件传递
            ViewParent parent = v.getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
            x = event.getX();
            y = event.getY();
            pendingCheckForLongPress = new CheckForLongPress(v);
            handler.postDelayed(pendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
        } else if (action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();
            if (isLongClick) {
                maskView.updateBounds((int) (event.getRawX() - this.x), (int) (event.getRawY() - this.y), v.getWidth(), v.getHeight());
                Logger.i(TAG, "cancel bounds:" + cancelView.getRealBounds() + " mask bounds:" + maskView.getRealBounds());
                maskView.setMarked(cancelView.getRealBounds().intersect(maskView.getRealBounds()));
            } else if (x < 0 || x > v.getWidth() || y < 0 || y > v.getHeight()) {
                handler.removeCallbacks(pendingCheckForLongPress);
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            ViewParent parent = v.getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(false);
            handler.removeCallbacks(pendingCheckForLongPress);
            if (isLongClick) {
                performDetachMirrorView(v);
                isLongClick = false;
            }
            hasBlockEvent = false;
            multiPointLock = false;
            isDragging = false;
        }
        return true;
    }

    private boolean isAttachedToActivity(View v) {
        Object viewRootImpl = ViewHelper.findViewRootImplByChildView(v.getParent());
        if (viewRootImpl == null) return false;
        WindowManager.LayoutParams mWindowAttributes = (WindowManager.LayoutParams) XposedHelpers.getObjectField(viewRootImpl, "mWindowAttributes");
        return mWindowAttributes != null && mWindowAttributes.type == WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
    }

    private void performAttachMirrorView(View v) {
        try {
            //Create mirror view and attach top view hierarchy
            Activity activity = Preconditions.checkNotNull(ViewHelper.getAttachedActivityFromView(v));

            ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();

            cancelView = new CancelView(v.getContext());
            cancelView.attachToContainer(container);

            maskView = MaskView.clone(v);
            maskView.attachToContainer(container);

            viewRule = ViewHelper.makeRule(v);

            snapshot = ViewHelper.snapshotView(ViewHelper.findTopParentViewByChildView(v));

            //Make original view invisible
            Logger.d(TAG, "[ApplyRule] start------------------------------------");
            ViewController.applyRule(v, viewRule);
            Logger.d(TAG, "[ApplyRule] end------------------------------------");
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void performDetachMirrorView(final View v) {
        Activity activity = ViewHelper.getAttachedActivityFromView(v);
        try {
            Preconditions.checkNotNull(activity);
        } catch (NullPointerException e) {
            return;
        }

        cancelView.detachFromContainer();
        if (maskView.isMarked()) {
            //丢弃该条规则
            try {
                maskView.detachFromContainer();
                viewRule.visibility = View.VISIBLE;
                ViewController.revokeRule(v, viewRule);
                if (Preconditions.checkBitmap(snapshot)) snapshot.recycle();
            } finally {
                snapshot = null;
                maskView = null;
                cancelView = null;
                viewRule = null;
            }
        } else {
            //Make original view gone
            viewRule.visibility = View.GONE;
            Logger.d(TAG, "[ApplyRule] start------------------------------------");
            ViewController.applyRule(v, viewRule);
            Logger.d(TAG, "[ApplyRule] end------------------------------------");
            GodModeManager manager = GodModeManager.getDefault();
            manager.writeRule(v.getContext().getPackageName(), viewRule, snapshot);
            if (Preconditions.checkBitmap(snapshot)) snapshot.recycle();

            ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();
            final ParticleView particleView = new ParticleView(activity);
            particleView.setDuration(1000);
            particleView.attachToContainer(container);
            particleView.setOnAnimationListener(new ParticleView.OnAnimationListener() {
                @Override
                public void onAnimationStart(View animView, Animator animation) {
                    maskView.detachFromContainer();
                }

                @Override
                public void onAnimationEnd(View animView, Animator animation) {
                    try {
                        particleView.detachFromContainer();
                    } finally {
                        snapshot = null;
                        maskView = null;
                        cancelView = null;
                        viewRule = null;
                    }
                }
            });
            particleView.boom(maskView);
        }
    }


    private class CheckForLongPress implements Runnable {

        private final WeakReference<View> viewRef;

        private CheckForLongPress(View view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        public void run() {
            View view = viewRef.get();
            if (view != null) {
                performAttachMirrorView(view);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                isLongClick = true;
            }
        }
    }
}
