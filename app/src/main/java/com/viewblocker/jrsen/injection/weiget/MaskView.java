package com.viewblocker.jrsen.injection.weiget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.viewblocker.jrsen.injection.ViewHelper;
import com.viewblocker.jrsen.injection.annotation.DisableHook;

/**
 * Created by jrsen on 17-10-13.
 */

@DisableHook
public final class MaskView extends View {

    private static final int MARK_COLOR = Color.argb(150, 139, 195, 75);
    private static final int SELECT_COLOR = Color.argb(150, 255, 0, 0);
    private boolean isMarked;

    public MaskView(Context context) {
        this(context, null);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void updatePosition(int newX, int newY) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        layoutParams.leftMargin = newX;
        layoutParams.topMargin = newY;
        requestLayout();
    }

    public void updatePosition(int x, int y, int w, int h) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        layoutParams.width = w;
        layoutParams.height = h;
        layoutParams.leftMargin = x;
        layoutParams.topMargin = y;
        requestLayout();
    }

    private Rect bounds = new Rect();

    public Rect getRealBounds() {
        int[] out = new int[2];
        getLocationInWindow(out);
        int l = out[0];
        int t = out[1];
        int r = l + getWidth();
        int b = t + getHeight();
        bounds.set(l, t, r, b);
        return bounds;
    }

    public void setMarked(boolean enable) {
        if (isMarked != enable) {
            if (isMarked = enable) {
                getBackground().setColorFilter(MARK_COLOR, PorterDuff.Mode.SRC_ATOP);
            } else {
                getBackground().clearColorFilter();
            }
        }
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected != isSelected()) {
            super.setSelected(selected);
            if (isSelected()) {
                getBackground().setColorFilter(SELECT_COLOR, PorterDuff.Mode.SRC_ATOP);
            } else {
                getBackground().clearColorFilter();
            }
        }
    }

    public boolean isMarked() {
        return isMarked;
    }

    public void attachToContainer(ViewGroup container) {
        container.addView(this);
    }

    public void detachFromContainer() {
        ViewGroup parent = (ViewGroup) getParent();
        parent.removeView(this);
    }

    public static MaskView clone(View view) {
        Bitmap bitmap = ViewHelper.cloneViewAsBitmap(view);
        Drawable drawable = new BitmapDrawable(view.getResources(), bitmap);
        MaskView maskView = new MaskView(view.getContext());
        maskView.setBackground(drawable);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(bitmap.getWidth(), bitmap.getHeight());
        maskView.setLayoutParams(layoutParams);

        int[] out = new int[2];
        view.getLocationOnScreen(out);
        maskView.updatePosition(out[0], out[1]);
        return maskView;
    }

    public static MaskView mask(View view) {
        MaskView maskView = new MaskView(view.getContext());
        maskView.setBackgroundColor(SELECT_COLOR);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(view.getWidth(), view.getHeight());
        maskView.setLayoutParams(layoutParams);

        int[] out = new int[2];
        view.getLocationInWindow(out);
        maskView.updatePosition(out[0], out[1]);
        return maskView;
    }

}
