package com.viewblocker.jrsen.injection.hook;

import android.animation.Animator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.widget.TooltipCompat;

import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.injection.BlockerInjector;
import com.viewblocker.jrsen.injection.ViewController;
import com.viewblocker.jrsen.injection.ViewHelper;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.injection.util.GmLayoutInflater;
import com.viewblocker.jrsen.injection.util.GmResources;
import com.viewblocker.jrsen.injection.util.Logger;
import com.viewblocker.jrsen.injection.util.Property;
import com.viewblocker.jrsen.injection.weiget.MaskView;
import com.viewblocker.jrsen.injection.weiget.ParticleView;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

import static com.viewblocker.jrsen.BlockerApplication.TAG;

public final class DispatchKeyEventHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean>, android.widget.SeekBar.OnSeekBarChangeListener {

    public static volatile boolean selecting;

    private final List<WeakReference<View>> mViewNodes = new ArrayList<>();
    private int mCurrentViewIndex = 0;

    private MaskView mMaskView;
    private View mNodeSelectorPanel;

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        if (BlockerInjector.switchProp.get()) {
            Activity activity = (Activity) param.thisObject;
            KeyEvent event = (KeyEvent) param.args[0];
            param.setResult(dispatchKeyEvent(activity, event));
        }
    }

    private boolean dispatchKeyEvent(final Activity activity, KeyEvent keyEvent) {
        Logger.d(TAG, keyEvent.toString());
        int action = keyEvent.getAction();
        int keyCode = keyEvent.getKeyCode();
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) && action == KeyEvent.ACTION_UP) {
            if (mMaskView == null) {
                showNodeSelectPanel(activity);
            } else {
                //hide node select panel
                dismissNodeSelectPanel();
            }
        }
        return true;
    }

    private void showNodeSelectPanel(final Activity activity) {
        //build view hierarchy tree
        mViewNodes.clear();
        mViewNodes.addAll(ViewHelper.buildViewNodes(activity.getWindow().getDecorView()));
        mCurrentViewIndex = 0;

        final WeakReference<View> viewWeakReference = mViewNodes.get(mCurrentViewIndex);
        mMaskView = MaskView.mask(viewWeakReference.get());
        mMaskView.setSelected(true);
        final ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();
        mMaskView.attachToContainer(container);
        try {
            LayoutInflater layoutInflater = GmLayoutInflater.from(activity);
            mNodeSelectorPanel = layoutInflater.inflate(R.layout.layout_node_selector, container, false);
            final SeekBar seekbar = mNodeSelectorPanel.findViewById(R.id.slider);
            seekbar.setMax(mViewNodes.size() - 1);
            seekbar.setOnSeekBarChangeListener(this);
            View btnBlock = mNodeSelectorPanel.findViewById(R.id.block);
            TooltipCompat.setTooltipText(btnBlock, GmResources.getText(activity, R.string.accessibility_block));
            btnBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        View view = mViewNodes.get(mCurrentViewIndex).get();
                        Logger.d(TAG, "removed view = " + view);
                        if (view != null) {
                            mMaskView.inflateView(view);
                            ViewRule viewRule = ViewHelper.makeRule(view);
                            viewRule.visibility = View.GONE;
                            Logger.d(TAG, "[ApplyRule] start------------------------------------");
                            ViewController.applyRule(view, viewRule);
                            Logger.d(TAG, "[ApplyRule] end------------------------------------");
                            Bitmap snapshot = ViewHelper.snapshotView(ViewHelper.findTopParentViewByChildView(view));
                            GodModeManager.getDefault().writeRule(activity.getPackageName(), viewRule, snapshot);
                            if (Preconditions.checkBitmap(snapshot)) snapshot.recycle();
                            final ParticleView particleView = new ParticleView(activity);
                            particleView.setDuration(1000);
                            particleView.attachToContainer(container);
                            particleView.setOnAnimationListener(new ParticleView.OnAnimationListener() {
                                @Override
                                public void onAnimationStart(View animView, Animator animation) {
                                    mMaskView.setSelected(false);
                                    mMaskView.updateBounds(new Rect());
                                }

                                @Override
                                public void onAnimationEnd(View animView, Animator animation) {
                                    particleView.detachFromContainer();
                                    selecting = false;
                                }
                            });
                            particleView.boom(mMaskView);
                        }
                        mViewNodes.remove(mCurrentViewIndex--);
                        seekbar.setMax(mViewNodes.size() - 1);
                    } catch (Exception e) {
                        selecting = false;
                        Toast.makeText(activity, GmResources.getString(activity, R.string.block_fail, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            container.addView(mNodeSelectorPanel);
            mNodeSelectorPanel.setAlpha(0);
            mNodeSelectorPanel.post(new Runnable() {
                @Override
                public void run() {
                    mNodeSelectorPanel.setTranslationX(mNodeSelectorPanel.getWidth() / 2.0f);
                    mNodeSelectorPanel.animate()
                            .alpha(1)
                            .translationX(0)
                            .setDuration(300)
                            .setInterpolator(new DecelerateInterpolator(1.0f))
                            .start();
                }
            });
            Logger.d(TAG, "add seek ok");
        } catch (Exception e) {
            //god mode package uninstalled?
            e.printStackTrace();
            Logger.e(TAG, "add seek fail", e);
        }
    }

    private void dismissNodeSelectPanel() {
        mMaskView.detachFromContainer();
        mMaskView = null;
        final View nodeSelectorPanel = mNodeSelectorPanel;
        nodeSelectorPanel.post(new Runnable() {
            @Override
            public void run() {
                nodeSelectorPanel.animate()
                        .alpha(0)
                        .translationX(nodeSelectorPanel.getWidth() / 2.0f)
                        .setDuration(250)
                        .setInterpolator(new AccelerateInterpolator(1.0f))
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                ViewGroup parent = (ViewGroup) nodeSelectorPanel.getParent();
                                if (parent != null) parent.removeView(nodeSelectorPanel);
                            }
                        })
                        .start();
            }
        });
        mNodeSelectorPanel = null;
    }

    @Override
    public void onPropertyChange(Boolean enable) {
        if (mMaskView != null) {
            dismissNodeSelectPanel();
        }
    }

    @Override
    public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mCurrentViewIndex = progress;
            View view = mViewNodes.get(mCurrentViewIndex).get();
            Logger.d(TAG, String.format(Locale.getDefault(), "progress=%d selected view=%s", progress, view));
            if (view != null) {
                mMaskView.setSelected(true);
                mMaskView.updateBounds(ViewHelper.getLocationInWindow(view));
            }
        }
    }

    @Override
    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
        mNodeSelectorPanel.setAlpha(0.2f);
    }

    @Override
    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
        mNodeSelectorPanel.setAlpha(1f);
    }
}
