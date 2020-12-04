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

import com.viewblocker.jrsen.injection.GodModeInjector;
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

import static com.viewblocker.jrsen.GodModeApplication.TAG;
import static com.viewblocker.jrsen.injection.ViewHelper.TAG_GM_CMP;

/**
 * Created by jrsen on 17-12-6.
 */

public final class DispatchTouchEventHook extends XC_MethodHook {

    public static volatile boolean mDragging;

    private float mX, mY;
    private Bitmap mSnapshot;
    private ViewRule mViewRule;
    private MaskView mMaskView;
    private CancelView mCancelView;
    private boolean mHasBlockEvent;
    private boolean mLongClick;
    private CheckForLongPress mPendingCheckForLongPress;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private volatile boolean multiPointLock;

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        View view = (View) param.thisObject;
        MotionEvent event = (MotionEvent) param.args[0];
        if (GodModeInjector.switchProp.get() && !TAG_GM_CMP.equals(view.getTag())) {
            if (DispatchKeyEventHook.mSelecting) {
                param.setResult(true);
            } else {
                param.setResult(dispatchTouchEvent(view, event));
            }
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
                if (!mHasBlockEvent) {
                    Toast.makeText(v.getContext(), "该控件属于悬浮窗暂不支持编辑", Toast.LENGTH_SHORT).show();
                    mHasBlockEvent = true;
                }
                return false;
            }
            mDragging = true;
            multiPointLock = true;//防止多个触点同时触发
            //防止列表控件拦截事件传递
            ViewParent parent = v.getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
            mX = event.getX();
            mY = event.getY();
            mPendingCheckForLongPress = new CheckForLongPress(v);
            mHandler.postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
        } else if (action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();
            if (mLongClick) {
                mMaskView.updateBounds((int) (event.getRawX() - this.mX), (int) (event.getRawY() - this.mY), v.getWidth(), v.getHeight());
                Logger.i(TAG, "cancel bounds:" + mCancelView.getRealBounds() + " mask bounds:" + mMaskView.getRealBounds());
                mMaskView.setMarked(mCancelView.getRealBounds().intersect(mMaskView.getRealBounds()));
            } else if (x < 0 || x > v.getWidth() || y < 0 || y > v.getHeight()) {
                mHandler.removeCallbacks(mPendingCheckForLongPress);
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            ViewParent parent = v.getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(false);
            mHandler.removeCallbacks(mPendingCheckForLongPress);
            if (mLongClick) {
                performDetachMirrorView(v);
                mLongClick = false;
            }
            mHasBlockEvent = false;
            multiPointLock = false;
            mDragging = false;
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

            mCancelView = new CancelView(v.getContext());
            mCancelView.attachToContainer(container);

            mMaskView = MaskView.clone(v);
            mMaskView.attachToContainer(container);

            mViewRule = ViewHelper.makeRule(v);

            mSnapshot = ViewHelper.snapshotView(ViewHelper.findTopParentViewByChildView(v));

            //Make original view invisible
            Logger.d(TAG, "[ApplyRule] start------------------------------------");
            ViewController.applyRule(v, mViewRule);
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

        mCancelView.detachFromContainer();
        if (mMaskView.isMarked()) {
            //丢弃该条规则
            try {
                mMaskView.detachFromContainer();
                mViewRule.visibility = View.VISIBLE;
                ViewController.revokeRule(v, mViewRule);
                if (Preconditions.checkBitmap(mSnapshot)) mSnapshot.recycle();
            } finally {
                mSnapshot = null;
                mMaskView = null;
                mCancelView = null;
                mViewRule = null;
            }
        } else {
            //Make original view gone
            mViewRule.visibility = View.GONE;
            Logger.d(TAG, "[ApplyRule] start------------------------------------");
            ViewController.applyRule(v, mViewRule);
            Logger.d(TAG, "[ApplyRule] end------------------------------------");
            GodModeManager manager = GodModeManager.getDefault();
            manager.writeRule(v.getContext().getPackageName(), mViewRule, mSnapshot);
            if (Preconditions.checkBitmap(mSnapshot)) mSnapshot.recycle();

            ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();
            final ParticleView particleView = new ParticleView(activity);
            particleView.setDuration(1000);
            particleView.attachToContainer(container);
            particleView.setOnAnimationListener(new ParticleView.OnAnimationListener() {
                @Override
                public void onAnimationStart(View animView, Animator animation) {
                    mMaskView.detachFromContainer();
                }

                @Override
                public void onAnimationEnd(View animView, Animator animation) {
                    try {
                        particleView.detachFromContainer();
                    } finally {
                        mSnapshot = null;
                        mMaskView = null;
                        mCancelView = null;
                        mViewRule = null;
                    }
                }
            });
            particleView.boom(mMaskView);
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
                mLongClick = true;
            }
        }
    }
}
