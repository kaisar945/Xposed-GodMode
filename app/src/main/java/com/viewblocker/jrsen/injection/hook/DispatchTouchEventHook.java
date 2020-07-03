package com.viewblocker.jrsen.injection.hook;

import android.animation.Animator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
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
import com.viewblocker.jrsen.service.GodModeManagerService;
import com.viewblocker.jrsen.injection.view.CancelView;
import com.viewblocker.jrsen.injection.view.MirrorView;
import com.viewblocker.jrsen.injection.view.ParticleView;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import java.lang.ref.SoftReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by jrsen on 17-12-6.
 */

public final class DispatchTouchEventHook extends XC_MethodHook {

    private float x, y;
    private Bitmap snapshot;
    private ViewRule viewRule;
    private MirrorView mirrorView;
    private CancelView cancelView;
    private boolean hasBlockEvent;

    private boolean isLongClick;
    private CheckForLongPress pendingCheckForLongPress;
    private Handler handler = new Handler(Looper.getMainLooper());

    private volatile boolean multiPointLock;

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        if (BlockerInjector.switchProp.get())
            param.setResult(onTouch((View) param.thisObject, (MotionEvent) param.args[0]));
    }

    private boolean onTouch(View v, MotionEvent event) {
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
                mirrorView.updatePosition((int) (event.getRawX() - this.x), (int) (event.getRawY() - this.y));
                if (Rect.intersects(cancelView.getRealBounds(), mirrorView.getRealBounds()) && !mirrorView.isMarked()) {
                    mirrorView.setMarked(true);
                } else if (!Rect.intersects(cancelView.getRealBounds(), mirrorView.getRealBounds()) && mirrorView.isMarked()) {
                    mirrorView.setMarked(false);
                }
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
        //Create mirror view and attach top view hierarchy
        Activity activity = ViewHelper.getAttachedActivityFromView(v);
        try {
            Preconditions.checkNotNull(activity);
        } catch (NullPointerException e) {
            return;
        }

        ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();

        cancelView = new CancelView(v.getContext());
        cancelView.attachToContainer(container);

        mirrorView = MirrorView.clone(v);
        mirrorView.attachToContainer(container);

        viewRule = ViewHelper.makeRule(v);

        snapshot = ViewHelper.snapshotView(ViewHelper.findTopParentViewByChildView(v));
        ViewHelper.markViewBounds(snapshot, viewRule.x, viewRule.y, viewRule.x + viewRule.width, viewRule.y + viewRule.height);

        //Make original view invisible
        ViewController.applyRule(v, viewRule);
    }

    private void performDetachMirrorView(final View v) {
        Activity activity = ViewHelper.getAttachedActivityFromView(v);
        try {
            Preconditions.checkNotNull(activity);
        } catch (NullPointerException e) {
            return;
        }

        cancelView.detachFromContainer();
        if (mirrorView.isMarked()) {
            //丢弃该条规则
            try {
                mirrorView.detachFromContainer();
                viewRule.visibility = View.VISIBLE;
                ViewController.revokeRule(v, viewRule);
            } finally {
                if (Preconditions.checkBitmap(snapshot)) {
                    snapshot.recycle();
                }
                snapshot = null;
                mirrorView = null;
                cancelView = null;
                viewRule = null;
            }

        } else {
            //Make original view gone
            viewRule.visibility = View.GONE;
            GodModeManagerService service = GodModeManagerService.getBridge();
            service.writeRule(v.getContext().getPackageName(), viewRule, snapshot);

            ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();
            final ParticleView particleView = new ParticleView(activity, 1000);
            particleView.attachToContainer(container);
            particleView.setOnAnimationListener(new ParticleView.OnAnimationListener() {
                @Override
                public void onAnimationStart(View v, Animator animation) {
                    mirrorView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationEnd(View v, Animator animation) {
                    try {
                        mirrorView.detachFromContainer();
                        particleView.detachFromContainer();
                        //应用规则
                        ViewController.applyRule(v, viewRule);
                    } finally {
                        mirrorView = null;
                        cancelView = null;
                        viewRule = null;
                    }
                }
            });
            particleView.boom(mirrorView);
        }
    }


    private class CheckForLongPress implements Runnable {

        private final SoftReference<View> viewRef;

        private CheckForLongPress(View view) {
            this.viewRef = new SoftReference<>(view);
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
