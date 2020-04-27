package com.viewblocker.jrsen.widget;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.viewblocker.jrsen.R;

/**
 * Created by jrsen on 17-11-18.
 */

public final class Snackbar {

    public static final int LENGTH_SHORT = -1;
    public static final int LENGTH_LONG = 0;

    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
    private static final int ANIMATION_DURATION = 250;
    private static final int SHORT_DURATION_MS = 1500;
    private static final int LONG_DURATION_MS = 2750;
    private final ViewGroup parent;
    private final View view;
    private int duration;

    private Snackbar(ViewGroup parent, View view) {
        this.parent = parent;
        this.view = view;
    }

    public static Snackbar make(@NonNull Activity container, @NonNull CharSequence text, int duration) {
        final LayoutInflater inflater = LayoutInflater.from(container);
        ViewGroup parent = (ViewGroup) container.findViewById(android.R.id.content);
        View view = inflater.inflate(R.layout.layout_snackbar, parent, false);
        Snackbar snackbar = new Snackbar(parent, view);
        snackbar.setText(text);
        snackbar.setDuration(duration);
        return snackbar;
    }

    public static Snackbar make(@NonNull Activity container, @StringRes int resId, int duration) {
        return make(container, container.getResources().getString(resId), duration);
    }

    private Snackbar setText(@NonNull CharSequence message) {
        TextView tv = (TextView) view.findViewById(R.id.snackbar_text);
        tv.setText(message);
        return this;
    }

    /**
     * @param resId    String resource to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    @NonNull
    public Snackbar setAction(@StringRes int resId, View.OnClickListener listener) {
        return setAction(view.getContext().getText(resId), listener);
    }

    @NonNull
    public Snackbar setAction(CharSequence text, final View.OnClickListener listener) {
        final TextView tv = (TextView) view.findViewById(R.id.snackbar_action);

        if (TextUtils.isEmpty(text) || listener == null) {
            tv.setVisibility(View.GONE);
            tv.setOnClickListener(null);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(text);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onClick(view);
                    // Now dismiss the Snackbar
                    dismiss();
                }
            });
        }
        return this;
    }

    private Snackbar setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public void show() {
        view.setVisibility(View.INVISIBLE);
        parent.addView(view);
        animateViewIn();
    }

    public void dismiss() {
        animateViewOut();
    }

    private void animateViewIn() {
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.VISIBLE);
                ViewCompat.setTranslationY(view, view.getHeight());
                ViewCompat.animate(view)
                        .translationY(0f)
                        .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                        .setDuration(ANIMATION_DURATION)
                        .setListener(new ViewPropertyAnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(View view) {
                            }

                            @Override
                            public void onAnimationEnd(View view) {
                                onViewShown();
                            }
                        }).start();
            }
        });
    }

    private void animateViewOut() {
        ViewCompat.animate(view)
                .translationY(view.getHeight())
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(View view) {
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        onViewHidden();
                    }
                }).start();
    }

    private Runnable timeout = new Runnable() {
        @Override
        public void run() {
            dismiss();
        }
    };

    private void onViewShown() {
        view.postDelayed(timeout, duration < 0 ? SHORT_DURATION_MS : LONG_DURATION_MS);
    }

    private void onViewHidden() {
        // Lastly, hide and remove the view from the parent (if attached)
        final ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }

}
