package com.viewblocker.jrsen.injection.hook;

import android.animation.Animator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.viewblocker.jrsen.injection.BlockerInjector;
import com.viewblocker.jrsen.injection.ViewHelper;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.injection.util.Property;
import com.viewblocker.jrsen.injection.weiget.MaskView;
import com.viewblocker.jrsen.injection.weiget.ParticleView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

import static com.viewblocker.jrsen.BlockerApplication.TAG;

public final class DispatchKeyEventHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {

    public static volatile boolean isSelecting;
    private int activityHashCode = 0;

    private List<WeakReference<View>> viewTree = new ArrayList<>();
    private int currentViewIndex = -1;
    private MaskView maskView;

    public DispatchKeyEventHook() {
        BlockerInjector.switchProp.addOnPropertyChangeListener(this);
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        if (BlockerInjector.switchProp.get()) {
            Activity activity = (Activity) param.thisObject;
            int currentActivityHashCode = System.identityHashCode(activity);
            if (activityHashCode != currentActivityHashCode) {
                activityHashCode = currentActivityHashCode;
                viewTree.clear();
                viewTree.addAll(ViewHelper.buildViewList(activity.getWindow().getDecorView()));
                currentViewIndex = -1;
            }
            param.setResult(dispatchKeyEvent(activity, (KeyEvent) param.args[0]));
        }
    }

    private boolean longPress;

    private boolean dispatchKeyEvent(Activity activity, KeyEvent keyEvent) {
        Logger.d(TAG, keyEvent.toString());
        int action = keyEvent.getAction();
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                isSelecting = true;
                longPress = longPress || keyEvent.getEventTime() - keyEvent.getDownTime() > ViewConfiguration.getLongPressTimeout();
                if (longPress) {
                    Logger.d(TAG, "long press");
                }
            } else if (action == KeyEvent.ACTION_UP) {
                if (longPress && maskView != null) {
                    Logger.d(TAG, "boom view");
                    ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();
                    final ParticleView particleView = new ParticleView(activity, 1000);
                    particleView.attachToContainer(container);
                    particleView.setOnAnimationListener(new ParticleView.OnAnimationListener() {
                        @Override
                        public void onAnimationStart(View animView, Animator animation) {
                            maskView.detachFromContainer();
                            maskView = null;
                        }

                        @Override
                        public void onAnimationEnd(View animView, Animator animation) {
                            particleView.detachFromContainer();
                        }
                    });
                    particleView.boom(maskView);
                } else {
                    View view = null;
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_VOLUME_UP:
                            currentViewIndex = Math.max(--currentViewIndex, 0);
                            view = viewTree.get(currentViewIndex).get();
                            break;
                        case KeyEvent.KEYCODE_VOLUME_DOWN:
                            currentViewIndex = Math.min(++currentViewIndex, viewTree.size() - 1);
                            view = viewTree.get(currentViewIndex).get();
                            break;
                    }
                    Logger.d(TAG, "selected view:" + view + " rect:" + ViewHelper.getLocationInWindow(view));
                    if (maskView == null) {
                        maskView = MaskView.mask(view);
//                        maskView.setSelected(true);
                        maskView.attachToContainer((ViewGroup) activity.getWindow().getDecorView());
                    }
                    Rect rect = ViewHelper.getLocationInWindow(view);
                    maskView.setBackgroundColor(Color.TRANSPARENT);
                    maskView.updatePosition(rect.left, rect.top, rect.width(), rect.height());
                    maskView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            maskView.setBackgroundColor(MaskView.SELECT_COLOR);
                        }
                    }, 50l);
                }
                longPress = false;
                isSelecting = false;
            }
        }
        return true;
    }

    @Override
    public void onPropertyChange(Boolean enable) {
        if (maskView != null) {
            maskView.detachFromContainer();
        }
    }
}
