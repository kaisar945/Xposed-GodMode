package com.kaisar.xposed.godmode.injection.hook;

import android.animation.Animator;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.Toast;

import com.kaisar.xposed.godmode.injection.ViewController;
import com.kaisar.xposed.godmode.injection.ViewHelper;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.injection.util.Property;
import com.kaisar.xposed.godmode.injection.weiget.CancelView;
import com.kaisar.xposed.godmode.injection.weiget.MaskView;
import com.kaisar.xposed.godmode.injection.weiget.ParticleView;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.Preconditions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;
import static com.kaisar.xposed.godmode.injection.ViewHelper.TAG_GM_CMP;
import static com.kaisar.xposed.godmode.injection.util.CommonUtils.recycleNullableBitmap;

/**
 * Created by jrsen on 17-12-6.
 */

public final class EventHandlerHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {

    private static final int MARK_COLOR = Color.argb(150, 139, 195, 75);
    private static final int OVERLAY_COLOR = Color.argb(150, 255, 0, 0);

    private boolean mIsInEditMode;
    private float mX, mY;
    private Bitmap mSnapshot;
    private ViewRule mViewRule;
    private MaskView mMaskView;
    private CancelView mCancelView;
    private boolean mHasBlockEvent;
    private boolean mLongClick;
    private CheckForLongPress mPendingCheckForLongPress;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private volatile boolean mMultiPointLock;
    private volatile boolean mDragging;

    private final List<WeakReference<View>> mViewNodes = new ArrayList<>();
    private int mCurrentViewIndex = 0;
    private volatile boolean mKeySelecting;

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        if (!mIsInEditMode) return;
        String methodName = param.method.getName();
        if ("dispatchKeyEvent".equals(methodName)) {
            if (!mDragging) {
                Activity activity = (Activity) param.thisObject;
                KeyEvent event = (KeyEvent) param.args[0];
                param.setResult(dispatchKeyEvent(activity, event));
            }
        } else if ("dispatchTouchEvent".equals(methodName)) {
            View view = (View) param.thisObject;
            MotionEvent event = (MotionEvent) param.args[0];
            if (mKeySelecting) {
                View selectedView = mViewNodes.get(mCurrentViewIndex).get();
                param.setResult(dispatchTouchEvent(selectedView, event));
            } else if (!TAG_GM_CMP.equals(view.getTag())) {
                param.setResult(dispatchTouchEvent(view, event));
            }
        }
    }

    private float mDeltaX, mDeltaY;

    private boolean dispatchTouchEvent(View v, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (mMultiPointLock) {
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
            mMultiPointLock = true;//防止多个触点同时触发
            //防止列表控件拦截事件传递
            ViewParent parent = v.getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
            Rect rect = ViewHelper.getLocationInWindow(v);
            mDeltaX = event.getRawX() - rect.left;
            mDeltaY = event.getRawY() - rect.top;
            mPendingCheckForLongPress = new CheckForLongPress(v);
            mHandler.postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
        } else if (action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();
            if (mLongClick) {
                mMaskView.updateOverlayBounds((int) (event.getRawX() - this.mDeltaX), (int) (event.getRawY() - this.mDeltaY), v.getWidth(), v.getHeight());
                mMaskView.setMarked(mCancelView.getRealBounds().intersect(mMaskView.getRealBounds()));
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
            mMultiPointLock = false;
            mDragging = false;
        }
        return true;
    }

    private boolean dispatchKeyEvent(final Activity activity, KeyEvent keyEvent) {
        Logger.d(TAG, keyEvent.toString());
        int action = keyEvent.getAction();
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!mKeySelecting && action == KeyEvent.ACTION_DOWN && keyEvent.getRepeatCount() == 0) {
                //build view tree
                ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                List<WeakReference<View>> viewNodes = ViewHelper.buildViewNodes(decorView);
                mViewNodes.clear();
                mViewNodes.addAll(viewNodes);
                mCurrentViewIndex = 0;
                mMaskView = MaskView.makeMaskView(activity);
                mMaskView.setMaskOverlay(OVERLAY_COLOR);
                View view = mViewNodes.get(mCurrentViewIndex).get();
                mMaskView.updateOverlayBounds(ViewHelper.getLocationInWindow(view));
                mMaskView.attachToContainer(decorView);
                mKeySelecting = true;
            } else if (action == KeyEvent.ACTION_DOWN) {
                mCurrentViewIndex = (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        ? Math.max(--mCurrentViewIndex, 0) : Math.min(++mCurrentViewIndex, mViewNodes.size() - 1);
                View view = mViewNodes.get(mCurrentViewIndex).get();
                mMaskView.updateOverlayBounds(ViewHelper.getLocationInWindow(view));
            }
            Logger.d(TAG, "node size=" + mViewNodes.size() + " index=" + mCurrentViewIndex + " key selecting=" + mKeySelecting);
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

            mCancelView = new CancelView(activity);
            mCancelView.attachToContainer(container);

            if (mKeySelecting && mMaskView != null) {
                mMaskView.setMaskOverlay(v);
                mMaskView.setMarkColor(MARK_COLOR);
                mMaskView.updateOverlayBounds(ViewHelper.getLocationInWindow(v));
            } else {
                mMaskView = MaskView.makeMaskView(activity);
                mMaskView.setMaskOverlay(v);
                mMaskView.setMarkColor(MARK_COLOR);
                mMaskView.updateOverlayBounds(ViewHelper.getLocationInWindow(v));
                mMaskView.attachToContainer(container);
            }

            mSnapshot = ViewHelper.snapshotView(ViewHelper.findTopParentViewByChildView(v));
            mViewRule = ViewHelper.makeRule(v);
            ViewController.applyRule(v, mViewRule);
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
                recycleNullableBitmap(mSnapshot);
            } finally {
                mSnapshot = null;
                mMaskView = null;
                mCancelView = null;
                mViewRule = null;
                mKeySelecting = false;
            }
        } else {
            ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();
            final ParticleView particleView = new ParticleView(activity);
            particleView.setDuration(1000);
            particleView.attachToContainer(container);
            particleView.setOnAnimationListener(new ParticleView.OnAnimationListener() {
                @Override
                public void onAnimationStart(View animView, Animator animation) {
                    //Make original view gone
                    mViewRule.visibility = View.GONE;
                    ViewController.applyRule(v, mViewRule);
                    GodModeManager.getDefault().writeRule(v.getContext().getPackageName(), mViewRule, mSnapshot);
                    recycleNullableBitmap(mSnapshot);
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
                        mKeySelecting = false;
                    }
                }
            });
            particleView.boom(mMaskView);
        }
    }

    @Override
    public void onPropertyChange(Boolean enable) {
        mIsInEditMode = enable;
        if (!enable) {
            mKeySelecting = false;
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
            Logger.d(TAG, "view =" + view);
            if (view != null) {
                Logger.d(TAG, "perform attach mirror view");
                performAttachMirrorView(view);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                mLongClick = true;
            }
        }
    }
}
