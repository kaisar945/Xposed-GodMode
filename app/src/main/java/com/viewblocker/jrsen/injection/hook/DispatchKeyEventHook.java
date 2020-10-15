package com.viewblocker.jrsen.injection.hook;

import android.app.Activity;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.injection.BlockerInjector;
import com.viewblocker.jrsen.injection.ViewHelper;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.injection.util.Property;
import com.viewblocker.jrsen.injection.weiget.MaskView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

import static com.viewblocker.jrsen.BlockerApplication.TAG;

public final class DispatchKeyEventHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {

    private int activityHashCode = 0;

    private List<WeakReference<View>> viewTree = new ArrayList<>();
    private int currentViewIndex = 0;
    private MaskView maskView;

    public DispatchKeyEventHook() {
        BlockerInjector.switchProp.addOnPropertyChangeListener(this);
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        Logger.d(TAG, "switchProp:" + BlockerInjector.switchProp.get());
        if (BlockerInjector.switchProp.get()) {
            Activity activity = (Activity) param.thisObject;
            int currentActivityHashCode = System.identityHashCode(activity);
            if (activityHashCode != currentActivityHashCode) {
                activityHashCode = currentActivityHashCode;
                viewTree.clear();
                viewTree.addAll(ViewHelper.buildViewList(activity.getWindow().getDecorView()));
                currentViewIndex = 0;
            }
            param.setResult(dispatchKeyEvent(activity, (KeyEvent) param.args[0]));
        }
    }

    private boolean longPress;

    private boolean dispatchKeyEvent(Activity activity, KeyEvent keyEvent) {
        Logger.d(TAG, keyEvent.toString());
        int action = keyEvent.getAction();
        int keyCode = keyEvent.getKeyCode();
        Logger.d(TAG, String.format("正在显示的Activity%s %d:", activity.toString(), currentViewIndex));
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                longPress = longPress || keyEvent.isLongPress();
            } else if (action == KeyEvent.ACTION_UP) {
                if (longPress) {
                    Logger.d(TAG, "boom view");
                } else {
                    View view = viewTree.get(currentViewIndex++).get();
                    Logger.d(TAG, "selected view:" + view + " rect:" + ViewHelper.getLocationOnScreen(view));
                    if (maskView == null) {
                        maskView = MaskView.mask(view);
                        maskView.setSelected(true);
                        maskView.attachToContainer((ViewGroup) activity.getWindow().getDecorView());
                    }
                    Rect rect = ViewHelper.getLocationInWindow(view);
                    maskView.updatePosition(rect.left, rect.top, rect.width(), rect.height());
                }
            }
            longPress = false;
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
